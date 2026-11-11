package com.esthetiadmin.backend.appointment.repository;

import com.esthetiadmin.backend.appointment.dto.AppointmentRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentResponse;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.esthetiadmin.backend.appointment.service.AppointmentService;
import com.esthetiadmin.backend.availability.dto.AvailabilityBlockRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityBlockResponse;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleRequest;
import com.esthetiadmin.backend.availability.service.AvailabilityService;
import com.esthetiadmin.backend.catalog.dto.CategoryRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingRequest;
import com.esthetiadmin.backend.catalog.service.CategoryService;
import com.esthetiadmin.backend.catalog.service.OfferingService;
import com.esthetiadmin.backend.client.dto.ClientRequest;
import com.esthetiadmin.backend.client.service.ClientService;
import com.esthetiadmin.backend.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against the real database: the V6 tables, the unique slot that prevents overlaps, the slots freed when an
 * appointment is cancelled and how appointments meet the schedule. Everything is rolled back.
 */
@SpringBootTest
@Transactional
class AppointmentDatabaseTest {

    /** Far enough ahead to be free of real data, on a Monday so the weekly rule applies. */
    private static final LocalDate DAY = nextMonday(LocalDate.now().plusYears(6));

    @Autowired
    private AppointmentService appointments;

    @Autowired
    private AvailabilityService availability;

    @Autowired
    private CategoryService categories;

    @Autowired
    private OfferingService offerings;

    @Autowired
    private ClientService clients;

    @Autowired
    private AppointmentSlotRepository slotRepository;

    private Long waxing;
    private Long facial;

    @BeforeEach
    void setUp() {
        Long waxingCategory = categories.create(new CategoryRequest("Zzdepilación", 30)).id();
        Long facialCategory = categories.create(new CategoryRequest("Zzfaciales", 60)).id();
        waxing = offerings.create(new OfferingRequest(waxingCategory, "Zzbozo", null, new BigDecimal("1000"), null)).id();
        facial = offerings.create(new OfferingRequest(facialCategory, "Zzlimpieza", null, new BigDecimal("2000"), null)).id();
        availability.replaceRules(List.of(new AvailabilityRuleRequest(1, LocalTime.of(9, 0), LocalTime.of(11, 0))));
    }

    @Test
    void anHourLongAppointmentHoldsTwoSlotsAndTheyCannotBeTakenAgain() {
        book(facial, "10:00");

        assertThat(slotRepository.findTakenStarts(DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay(), null))
                .containsExactlyInAnyOrder(DAY.atTime(10, 0), DAY.atTime(10, 30));
        assertThatThrownBy(() -> book(waxing, "10:30"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ese horario ya está ocupado");
        assertThat(appointments.availability(waxing, DAY, null).times()).containsExactly("09:00", "09:30");
        assertThat(appointments.availability(facial, DAY, null).times()).containsExactly("09:00");
    }

    @Test
    void cancellingFreesTheTimeForSomeoneElse() {
        AppointmentResponse first = book(facial, "10:00");

        appointments.changeStatus(first.id(), AppointmentStatus.CANCELLED);

        assertThat(book(waxing, "10:30").status()).isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    void movingAnAppointmentReleasesItsOldSlots() {
        AppointmentResponse booked = book(facial, "09:00");

        appointments.update(booked.id(), request(facial, "10:00", null));

        assertThat(appointments.availability(waxing, DAY, null).times()).containsExactly("09:00", "09:30");
        assertThat(appointments.availability(facial, DAY, booked.id()).times())
                .containsExactly("09:00", "09:30", "10:00");
    }

    @Test
    void attendedNeedsTheClientAndTheAgendaListsTheDay() {
        AppointmentResponse booked = book(waxing, "09:30");
        Long client = clients.create(new ClientRequest("Zzcarina", "Zzdiaz", null, null, null, null)).id();

        appointments.linkClient(booked.id(), client);
        appointments.changeStatus(booked.id(), AppointmentStatus.ATTENDED);

        assertThat(appointments.search(DAY, DAY, null))
                .extracting(AppointmentResponse::status, AppointmentResponse::clientId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(AppointmentStatus.ATTENDED, client));
        assertThatThrownBy(() -> clients.delete(client))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("turnos");
    }

    @Test
    void aBlockReportsTheConfirmedAppointmentsLeftInsideAndHidesItsTimes() {
        AppointmentResponse booked = book(waxing, "10:00");

        AvailabilityBlockResponse block = availability.createBlock(
                new AvailabilityBlockRequest(DAY, DAY, LocalTime.of(10, 0), LocalTime.of(11, 0), null));

        assertThat(block.affectedAppointments()).extracting(a -> a.id()).containsExactly(booked.id());
        assertThat(appointments.availability(waxing, DAY, null).times()).containsExactly("09:00", "09:30");
    }

    @Test
    void aServiceWithAppointmentsCannotBeDeleted() {
        book(waxing, "09:00");

        assertThatThrownBy(() -> offerings.delete(waxing))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("turnos");
    }

    private AppointmentResponse book(Long serviceId, String time) {
        return appointments.create(request(serviceId, time, null));
    }

    private static AppointmentRequest request(Long serviceId, String time, Long clientId) {
        LocalDateTime startsAt = DAY.atTime(LocalTime.parse(time));
        return new AppointmentRequest(serviceId, startsAt, clientId, "Zzana", "Zzpérez", "3415551234", null, null);
    }

    private static LocalDate nextMonday(LocalDate from) {
        LocalDate day = from;
        while (day.getDayOfWeek().getValue() != 1) {
            day = day.plusDays(1);
        }
        return day;
    }
}
