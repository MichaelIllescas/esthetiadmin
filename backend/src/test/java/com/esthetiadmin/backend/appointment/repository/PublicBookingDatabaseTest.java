package com.esthetiadmin.backend.appointment.repository;

import com.esthetiadmin.backend.appointment.dto.PublicBookingRequest;
import com.esthetiadmin.backend.appointment.service.PublicBookingService;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleRequest;
import com.esthetiadmin.backend.availability.service.AvailabilityService;
import com.esthetiadmin.backend.catalog.dto.BookableCategory;
import com.esthetiadmin.backend.catalog.dto.CategoryRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingRequest;
import com.esthetiadmin.backend.catalog.service.CategoryService;
import com.esthetiadmin.backend.catalog.service.OfferingService;
import com.esthetiadmin.backend.common.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The public booking against the real database: what it offers, what it books and the per-phone cap. Rolled back. */
@SpringBootTest
@Transactional
class PublicBookingDatabaseTest {

    /** The next Monday, always inside the 60-day window; the weekly rule opens Mondays only. */
    private static final LocalDate MONDAY = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

    @Autowired
    private PublicBookingService booking;

    @Autowired
    private AvailabilityService availability;

    @Autowired
    private CategoryService categories;

    @Autowired
    private OfferingService offerings;

    private Long bookable;
    private Long hidden;

    @BeforeEach
    void setUp() {
        Long category = categories.create(new CategoryRequest("Zzdepilación", 30)).id();
        bookable = offerings.create(new OfferingRequest(category, "Zzbozo", null, new BigDecimal("1000"), true)).id();
        hidden = offerings.create(new OfferingRequest(category, "Zzláser", null, new BigDecimal("9000"), false)).id();
        availability.replaceRules(List.of(new AvailabilityRuleRequest(1, LocalTime.of(9, 0), LocalTime.of(10, 30))));
    }

    @Test
    void offersOnlyBookableServices() {
        BookableCategory category = booking.services().stream()
                .filter(candidate -> candidate.categoryName().equals("Zzdepilación"))
                .findFirst().orElseThrow();

        assertThat(category.services()).extracting(service -> service.id()).containsExactly(bookable);
        assertThat(category.services()).extracting(service -> service.id()).doesNotContain(hidden);
    }

    @Test
    void aBookedTimeIsNoLongerOfferedAndThePhoneIsCappedAtTwo() {
        assertThat(booking.availability(bookable, MONDAY).times()).containsExactly("09:00", "09:30", "10:00");

        booking.book(request("09:00", "341 555-1234"), "203.0.113.10");
        booking.book(request("09:30", "3415551234"), "203.0.113.11");

        assertThat(booking.availability(bookable, MONDAY).times()).containsExactly("10:00");
        assertThat(booking.availableDays(bookable, java.time.YearMonth.from(MONDAY)).days()).contains(MONDAY);
        assertThatThrownBy(() -> booking.book(request("10:00", "(341) 555 1234"), "203.0.113.12"))
                .isInstanceOf(ConflictException.class)
                .hasMessageStartingWith("Ya tenés 2 turnos reservados");
    }

    private PublicBookingRequest request(String time, String phone) {
        return new PublicBookingRequest(bookable, MONDAY.atTime(LocalTime.parse(time)), "Zzana", "Zzpérez", phone, null);
    }
}
