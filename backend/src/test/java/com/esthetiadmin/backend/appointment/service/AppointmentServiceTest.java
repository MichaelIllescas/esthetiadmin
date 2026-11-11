package com.esthetiadmin.backend.appointment.service;

import com.esthetiadmin.backend.appointment.dto.AppointmentRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentResponse;
import com.esthetiadmin.backend.appointment.dto.DayAvailabilityResponse;
import com.esthetiadmin.backend.appointment.entity.Appointment;
import com.esthetiadmin.backend.appointment.entity.AppointmentSlot;
import com.esthetiadmin.backend.appointment.entity.AppointmentSource;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.esthetiadmin.backend.appointment.repository.AppointmentRepository;
import com.esthetiadmin.backend.appointment.repository.AppointmentSlotRepository;
import com.esthetiadmin.backend.availability.dto.DaySchedule;
import com.esthetiadmin.backend.availability.dto.TimeRange;
import com.esthetiadmin.backend.availability.service.AvailabilityLookup;
import com.esthetiadmin.backend.catalog.dto.OfferingInfo;
import com.esthetiadmin.backend.catalog.service.CatalogLookup;
import com.esthetiadmin.backend.client.dto.ClientContact;
import com.esthetiadmin.backend.client.service.ClientLookup;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import com.esthetiadmin.backend.notification.dto.AppointmentMail;
import com.esthetiadmin.backend.notification.dto.NoticeType;
import com.esthetiadmin.backend.notification.service.AppointmentNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);
    private static final LocalDateTime TOMORROW_10 = NOW.plusDays(1).withHour(10);
    private static final OfferingInfo WAXING = new OfferingInfo(10L, "Bozo", 1L, "Depilación", true, true, 30);
    private static final OfferingInfo FACIAL = new OfferingInfo(20L, "Limpieza", 2L, "Faciales", true, true, 60);
    private static final OfferingInfo OFF = new OfferingInfo(30L, "Viejo", 2L, "Faciales", false, true, 60);

    private AppointmentRepository appointmentRepository;
    private AppointmentSlotRepository slotRepository;
    private CatalogLookup catalog;
    private ClientLookup clients;
    private AvailabilityLookup availability;
    private AppointmentNotifier notifier;
    private AppointmentService service;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        slotRepository = mock(AppointmentSlotRepository.class);
        catalog = mock(CatalogLookup.class);
        clients = mock(ClientLookup.class);
        availability = mock(AvailabilityLookup.class);
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        notifier = mock(AppointmentNotifier.class);
        service = new AppointmentService(appointmentRepository, new AppointmentSlots(slotRepository), catalog, clients,
                availability, notifier, clock);

        for (OfferingInfo offering : List.of(WAXING, FACIAL, OFF)) {
            when(catalog.findOffering(offering.id())).thenReturn(Optional.of(offering));
        }
        when(catalog.findOfferings(any())).thenReturn(Map.of(10L, WAXING, 20L, FACIAL, 30L, OFF));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(call -> {
            Appointment appointment = call.getArgument(0);
            ReflectionTestUtils.setField(appointment, "id", 52L);
            return appointment;
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void createConfirmsAnAdminAppointmentHoldingOneSlotPerHalfHour() {
        ArgumentCaptor<List<AppointmentSlot>> slots = ArgumentCaptor.forClass(List.class);
        AppointmentResponse response = service.create(request(20L, TOMORROW_10, null, "Carina", "Diaz", "341 555-1234"));

        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(response.source()).isEqualTo(AppointmentSource.ADMIN);
        assertThat(response.endsAt()).isEqualTo(TOMORROW_10.plusMinutes(60));
        assertThat(response.phone()).isEqualTo("3415551234");
        assertThat(response.serviceName()).isEqualTo("Limpieza");
        verify(slotRepository).saveAllAndFlush(slots.capture());
        assertThat(slots.getValue()).extracting(AppointmentSlot::getSlotStart)
                .containsExactly(TOMORROW_10, TOMORROW_10.plusMinutes(30));
    }

    @Test
    void createFillsTheMissingContactFromTheClient() {
        when(clients.findContact(7L)).thenReturn(Optional.of(new ClientContact(7L, "Carina", "Diaz", "3415551234", null)));

        AppointmentResponse response = service.create(request(10L, TOMORROW_10, 7L, null, " ", null));

        assertThat(response.firstName()).isEqualTo("Carina");
        assertThat(response.lastName()).isEqualTo("Diaz");
        assertThat(response.phone()).isEqualTo("3415551234");
        assertThat(response.clientId()).isEqualTo(7L);
    }

    @Test
    void createNeedsAPhoneEvenForAClientWithoutOne() {
        when(clients.findContact(7L)).thenReturn(Optional.of(new ClientContact(7L, "Carina", "Diaz", null, null)));

        assertRejected(() -> service.create(request(10L, TOMORROW_10, 7L, null, null, null)),
                BadRequestException.class, "El teléfono es obligatorio");
    }

    @Test
    void createRejectsAnUnknownClient() {
        when(clients.findContact(7L)).thenReturn(Optional.empty());

        assertRejected(() -> service.create(request(10L, TOMORROW_10, 7L, null, null, null)),
                NotFoundException.class, "El cliente no existe");
    }

    @Test
    void createRejectsATimeOutsideTheGridOrInThePast() {
        assertRejected(() -> service.create(request(10L, TOMORROW_10.plusMinutes(15), null, "A", "B", "34155512")),
                BadRequestException.class, "El turno tiene que empezar en punto o y media");
        assertRejected(() -> service.create(request(10L, NOW.withHour(11), null, "A", "B", "34155512")),
                BadRequestException.class, "No se puede dar un turno en un horario que ya pasó");
    }

    @Test
    void createRejectsAServiceThatCannotBeOffered() {
        assertRejected(() -> service.create(request(30L, TOMORROW_10, null, "A", "B", "34155512")),
                ConflictException.class, "El servicio no está disponible");
    }

    @Test
    void createRejectsATakenTime() {
        when(slotRepository.existsBySlotStartIn(anyCollection())).thenReturn(true);

        assertRejected(() -> service.create(request(10L, TOMORROW_10, null, "A", "B", "34155512")),
                ConflictException.class, "Ese horario ya está ocupado");
        verify(slotRepository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void aRaceForTheSameTimeEndsAsAConflict() {
        when(slotRepository.saveAllAndFlush(anyList())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertRejected(() -> service.create(request(10L, TOMORROW_10, null, "A", "B", "34155512")),
                ConflictException.class, "Ese horario ya está ocupado");
    }

    @Test
    void updateMovesTheAppointmentAndFreesItsPreviousTime() {
        Appointment appointment = existing(AppointmentStatus.CONFIRMED, 10L);

        AppointmentResponse response = service.update(52L,
                request(10L, TOMORROW_10.plusHours(2), null, "Carina", "Diaz", "3415551234"));

        assertThat(response.startsAt()).isEqualTo(TOMORROW_10.plusHours(2));
        verify(slotRepository).deleteByAppointmentId(52L);
        assertThat(appointment.getEndsAt()).isEqualTo(TOMORROW_10.plusHours(2).plusMinutes(30));
    }

    @Test
    void updateKeepsItsOwnServiceEvenIfItWasTurnedOff() {
        existing(AppointmentStatus.CONFIRMED, 30L);

        assertThat(service.update(52L, request(30L, TOMORROW_10, null, "A", "B", "34155512")).serviceId())
                .isEqualTo(30L);
    }

    @Test
    void updateOnlyChangesConfirmedAppointments() {
        existing(AppointmentStatus.CANCELLED, 10L);

        assertRejected(() -> service.update(52L, request(10L, TOMORROW_10, null, "A", "B", "34155512")),
                ConflictException.class, "Solo se puede modificar un turno confirmado");
    }

    @Test
    void cancellingFreesTheTime() {
        existing(AppointmentStatus.CONFIRMED, 10L);

        assertThat(service.changeStatus(52L, AppointmentStatus.CANCELLED).status()).isEqualTo(AppointmentStatus.CANCELLED);
        verify(slotRepository).deleteByAppointmentId(52L);
    }

    @Test
    void attendedNeedsALinkedClientAndKeepsTheTime() {
        Appointment appointment = existing(AppointmentStatus.CONFIRMED, 10L);
        assertRejected(() -> service.changeStatus(52L, AppointmentStatus.ATTENDED),
                ConflictException.class, "Vinculá el turno con un cliente antes de marcarlo atendido");

        appointment.linkClient(7L);
        assertThat(service.changeStatus(52L, AppointmentStatus.ATTENDED).status()).isEqualTo(AppointmentStatus.ATTENDED);
        verify(slotRepository, never()).deleteByAppointmentId(any());
    }

    @Test
    void aFinishedAppointmentCannotChangeAgain() {
        existing(AppointmentStatus.NO_SHOW, 10L);

        assertRejected(() -> service.changeStatus(52L, AppointmentStatus.CANCELLED),
                ConflictException.class, "El turno ya no está confirmado");
        assertRejected(() -> service.changeStatus(52L, AppointmentStatus.CONFIRMED),
                BadRequestException.class, "Un turno no puede volver a quedar confirmado");
    }

    @Test
    void linkClientChecksTheClientExists() {
        Appointment appointment = existing(AppointmentStatus.CONFIRMED, 10L);
        when(clients.exists(7L)).thenReturn(true);

        assertThat(service.linkClient(52L, 7L).clientId()).isEqualTo(7L);
        assertThat(appointment.getClientId()).isEqualTo(7L);
        assertRejected(() -> service.linkClient(52L, 8L), NotFoundException.class, "El cliente no existe");
    }

    @Test
    void searchRejectsAnInvertedOrTooLongRange() {
        LocalDate today = NOW.toLocalDate();
        assertRejected(() -> service.search(today, today.minusDays(1), null), BadRequestException.class, "La fecha");
        assertRejected(() -> service.search(today, today.plusDays(63), null), BadRequestException.class,
                "El rango de fechas no puede superar los 62 días");
    }

    @Test
    void availabilityAppliesTheScheduleTheTakenSlotsAndTheDuration() {
        LocalDate tomorrow = NOW.toLocalDate().plusDays(1);
        when(availability.scheduleOf(tomorrow)).thenReturn(
                new DaySchedule(List.of(new TimeRange(9 * 60, 11 * 60)), List.of()));
        when(slotRepository.findTakenStarts(tomorrow.atStartOfDay(), tomorrow.plusDays(1).atStartOfDay(), 52L))
                .thenReturn(List.of(tomorrow.atTime(10, 0)));

        DayAvailabilityResponse response = service.availability(20L, tomorrow, 52L);

        assertThat(response.durationMinutes()).isEqualTo(60);
        assertThat(response.times()).containsExactly("09:00");
    }

    private Appointment existing(AppointmentStatus status, Long serviceId) {
        Appointment appointment = new Appointment(AppointmentSource.PUBLIC, NOW.minusDays(1));
        ReflectionTestUtils.setField(appointment, "id", 52L);
        appointment.schedule(serviceId, TOMORROW_10, TOMORROW_10.plusMinutes(30));
        appointment.changeContact(null, "Carina", "Diaz", "3415551234", null, null);
        appointment.changeStatus(status);
        when(appointmentRepository.findById(52L)).thenReturn(Optional.of(appointment));
        return appointment;
    }

    private static AppointmentRequest request(Long serviceId, LocalDateTime startsAt, Long clientId, String firstName,
                                              String lastName, String phone) {
        return new AppointmentRequest(serviceId, startsAt, clientId, firstName, lastName, phone, null, null);
    }

    private static void assertRejected(Runnable call, Class<? extends Throwable> type, String message) {
        assertThatThrownBy(call::run).isInstanceOf(type).hasMessageStartingWith(message);
    }

    @Test
    void anAppointmentWithoutEmailTakesTheOneOfItsClient() {
        when(clients.findContact(7L)).thenReturn(Optional.of(
                new ClientContact(7L, "Carina", "Diaz", "3415551234", "carina@mail.com")));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(call -> call.getArgument(0));

        AppointmentResponse response = service.create(request(10L, TOMORROW_10, 7L, null, null, null));

        assertThat(response.email()).isEqualTo("carina@mail.com");
    }

    @Test
    void theEmailOfTheAppointmentIsCopiedToItsClient() {
        when(clients.findContact(7L)).thenReturn(Optional.of(new ClientContact(7L, "Carina", "Diaz", "3415551234", null)));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(call -> call.getArgument(0));

        service.create(new AppointmentRequest(10L, TOMORROW_10, 7L, null, null, null, null, " Carina@Mail.com "));

        verify(clients).fillMissingEmail(7L, "carina@mail.com");
    }

    @Test
    void aNoticeNeedsAnEmail() {
        Appointment appointment = confirmedWithEmail(null);

        assertRejected(() -> service.sendNotice(52L, NoticeType.BOOKED), BadRequestException.class,
                "La clienta no tiene email cargado");
        verify(notifier, never()).notifyClient(any(), any(), any());
        assertThat(appointment.getEmail()).isNull();
    }

    @Test
    void aNoticeMustMatchTheStatusOfTheAppointment() {
        confirmedWithEmail("carina@mail.com");

        assertRejected(() -> service.sendNotice(52L, NoticeType.CANCELLED), ConflictException.class,
                "Ese aviso no corresponde al estado del turno");
    }

    @Test
    void sendsTheNoticeTheOwnerAskedFor() {
        confirmedWithEmail("carina@mail.com");

        service.sendNotice(52L, NoticeType.RESCHEDULED);

        verify(notifier).notifyClient(52L, new AppointmentMail("Carina", "Diaz", "3415551234", "carina@mail.com",
                "Bozo", TOMORROW_10), NoticeType.RESCHEDULED);
    }

    private Appointment confirmedWithEmail(String email) {
        Appointment appointment = new Appointment(AppointmentSource.ADMIN, NOW);
        appointment.schedule(10L, TOMORROW_10, TOMORROW_10.plusMinutes(30));
        appointment.changeContact(null, "Carina", "Diaz", "3415551234", email, null);
        when(appointmentRepository.findById(52L)).thenReturn(Optional.of(appointment));
        return appointment;
    }
}
