package com.esthetiadmin.backend.appointment.service;

import com.esthetiadmin.backend.appointment.dto.PublicBookingRequest;
import com.esthetiadmin.backend.appointment.dto.PublicBookingResponse;
import com.esthetiadmin.backend.appointment.entity.Appointment;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.esthetiadmin.backend.appointment.repository.AppointmentRepository;
import com.esthetiadmin.backend.appointment.repository.AppointmentSlotRepository;
import com.esthetiadmin.backend.availability.dto.DaySchedule;
import com.esthetiadmin.backend.availability.dto.TimeRange;
import com.esthetiadmin.backend.availability.service.AvailabilityLookup;
import com.esthetiadmin.backend.catalog.dto.OfferingInfo;
import com.esthetiadmin.backend.catalog.service.CatalogLookup;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import com.esthetiadmin.backend.common.exception.TooManyRequestsException;
import com.esthetiadmin.backend.common.ratelimit.RateLimiter;
import com.esthetiadmin.backend.notification.dto.AppointmentMail;
import com.esthetiadmin.backend.notification.dto.PublicBookingCreated;
import com.esthetiadmin.backend.notification.service.AppointmentNotifier;
import com.esthetiadmin.backend.settings.dto.BusinessContact;
import com.esthetiadmin.backend.settings.service.SettingsLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicBookingServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);
    private static final LocalDate TOMORROW = NOW.toLocalDate().plusDays(1);
    private static final DaySchedule MORNING = new DaySchedule(List.of(new TimeRange(9 * 60, 11 * 60)), List.of());
    private static final OfferingInfo WAXING = new OfferingInfo(10L, "Bozo", 1L, "Depilación", true, true, 30);
    private static final OfferingInfo HIDDEN = new OfferingInfo(11L, "Láser", 1L, "Depilación", true, false, 30);
    private static final OfferingInfo OFF = new OfferingInfo(12L, "Viejo", 1L, "Depilación", false, true, 30);
    private static final String IP = "203.0.113.7";

    private AppointmentRepository appointmentRepository;
    private AppointmentSlotRepository slotRepository;
    private AvailabilityLookup availability;
    private SettingsLookup settings;
    private AppointmentNotifier notifier;
    private ApplicationEventPublisher events;
    private PublicBookingService service;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        slotRepository = mock(AppointmentSlotRepository.class);
        CatalogLookup catalog = mock(CatalogLookup.class);
        availability = mock(AvailabilityLookup.class);
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        settings = mock(SettingsLookup.class);
        when(settings.contact()).thenReturn(new BusinessContact(null, null));
        notifier = mock(AppointmentNotifier.class);
        events = mock(ApplicationEventPublisher.class);
        service = new PublicBookingService(appointmentRepository, new AppointmentSlots(slotRepository), catalog,
                availability, clock, new RateLimiter(2, Duration.ofHours(1), clock), settings, notifier, events);

        for (OfferingInfo offering : List.of(WAXING, HIDDEN, OFF)) {
            when(catalog.findOffering(offering.id())).thenReturn(Optional.of(offering));
        }
        when(availability.scheduleOf(any())).thenReturn(MORNING);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(call -> {
            Appointment appointment = call.getArgument(0);
            ReflectionTestUtils.setField(appointment, "id", 52L);
            return appointment;
        });
    }

    @Test
    void booksAFreeTimeAsAConfirmedPublicAppointment() {
        PublicBookingResponse response = service.book(request(10L, TOMORROW.atTime(9, 30), "341 555-1234"), IP);

        assertThat(response.serviceName()).isEqualTo("Bozo");
        assertThat(response.endsAt()).isEqualTo(TOMORROW.atTime(10, 0));
        assertThat(response.firstName()).isEqualTo("Carina");
        verify(appointmentRepository).save(org.mockito.ArgumentMatchers.argThat(appointment ->
                appointment.getSource().name().equals("PUBLIC") && appointment.getPhone().equals("3415551234")
                        && appointment.getStatus() == AppointmentStatus.CONFIRMED));
    }

    @Test
    void aTimeOutsideTheOpeningHoursOrAlreadyTakenIsRejected() {
        assertThatThrownBy(() -> service.book(request(10L, TOMORROW.atTime(15, 0), "3415551234"), IP))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ese horario acaba de ocuparse, elegí otro");

        when(slotRepository.findTakenStarts(any(), any(), eq(null))).thenReturn(List.of(TOMORROW.atTime(9, 30)));
        assertThatThrownBy(() -> service.book(request(10L, TOMORROW.atTime(9, 30), "3415551234"), "198.51.100.1"))
                .isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void aServiceThatIsNotBookableLooksLikeOneThatDoesNotExist() {
        assertThatThrownBy(() -> service.book(request(11L, TOMORROW.atTime(9, 0), "3415551234"), IP))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("El servicio no existe o no se puede reservar online");
        assertThatThrownBy(() -> service.availability(12L, TOMORROW)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void onlyDaysFromTodayUpToSixtyDaysAheadCanBeBooked() {
        assertThatThrownBy(() -> service.availability(10L, NOW.toLocalDate().minusDays(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Elegí un día entre hoy y los próximos 60 días");
        assertThatThrownBy(() -> service.availability(10L, NOW.toLocalDate().plusDays(61)))
                .isInstanceOf(BadRequestException.class);
        assertThat(service.availability(10L, NOW.toLocalDate().plusDays(60)).times()).hasSize(4);
    }

    @Test
    void todayOnlyOffersTimesThatHaveNotStarted() {
        assertThat(service.availability(10L, NOW.toLocalDate()).times()).isEmpty();
    }

    @Test
    void aPhoneWithTwoUpcomingAppointmentsCannotBookAnother() {
        when(appointmentRepository.countByPhoneAndStatusAndStartsAtAfter("3415551234", AppointmentStatus.CONFIRMED, NOW))
                .thenReturn(2L);

        assertThatThrownBy(() -> service.book(request(10L, TOMORROW.atTime(9, 0), "341-555-1234"), IP))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya tenés 2 turnos reservados. Para otro, comunicate con el centro");
    }

    @Test
    void theLimitMessageIncludesTheContactPhoneWhenThereIsOne() {
        when(settings.contact()).thenReturn(new BusinessContact(null, "3415559876"));
        when(appointmentRepository.countByPhoneAndStatusAndStartsAtAfter("3415551234", AppointmentStatus.CONFIRMED, NOW))
                .thenReturn(2L);

        assertThatThrownBy(() -> service.book(request(10L, TOMORROW.atTime(9, 0), "3415551234"), IP))
                .hasMessage("Ya tenés 2 turnos reservados. Para otro, comunicate con el centro al 3415559876");
    }

    @Test
    void aBookingWithEmailAsksForItsConfirmation() {
        when(notifier.enabled()).thenReturn(true);
        PublicBookingRequest withEmail = new PublicBookingRequest(10L, TOMORROW.atTime(9, 30), "Carina", "Diaz",
                "3415551234", " Carina@Mail.com ");

        PublicBookingResponse response = service.book(withEmail, IP);

        assertThat(response.emailSent()).isTrue();
        verify(events).publishEvent(new PublicBookingCreated(52L, new AppointmentMail("Carina", "Diaz",
                "3415551234", "carina@mail.com", "Bozo", TOMORROW.atTime(9, 30))));
    }

    @Test
    void aBookingWithoutEmailStillNotifiesTheOwner() {
        when(notifier.enabled()).thenReturn(true);

        PublicBookingResponse response = service.book(request(10L, TOMORROW.atTime(9, 30), "3415551234"), IP);

        assertThat(response.emailSent()).isFalse();
        verify(events).publishEvent(any(PublicBookingCreated.class));
    }

    @Test
    void theSameAddressCannotAskMoreThanTheHourlyLimit() {
        service.book(request(10L, TOMORROW.atTime(9, 0), "3415551234"), IP);
        assertThatThrownBy(() -> service.book(request(10L, TOMORROW.atTime(15, 0), "3415551234"), IP))
                .isInstanceOf(ConflictException.class);

        assertThatThrownBy(() -> service.book(request(10L, TOMORROW.atTime(9, 30), "3415551234"), IP))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("Hiciste demasiados pedidos de turno. Probá de nuevo más tarde");
    }

    @Test
    void theMonthOnlyListsDaysInsideTheWindowWithAFreeTime() {
        LocalDate first = NOW.toLocalDate();
        Map<LocalDate, DaySchedule> schedule = new LinkedHashMap<>();
        for (LocalDate day = first; !day.isAfter(YearMonth.from(first).atEndOfMonth()); day = day.plusDays(1)) {
            schedule.put(day, day.equals(TOMORROW) || day.equals(first) ? MORNING
                    : new DaySchedule(List.of(), List.of()));
        }
        when(availability.scheduleBetween(first, YearMonth.from(first).atEndOfMonth())).thenReturn(schedule);

        assertThat(service.availableDays(10L, YearMonth.from(first)).days()).containsExactly(TOMORROW);
        assertThat(service.availableDays(10L, YearMonth.from(first).minusMonths(1)).days()).isEmpty();
    }

    private static PublicBookingRequest request(Long serviceId, LocalDateTime startsAt, String phone) {
        return new PublicBookingRequest(serviceId, startsAt, " Carina ", "Diaz", phone, null);
    }
}
