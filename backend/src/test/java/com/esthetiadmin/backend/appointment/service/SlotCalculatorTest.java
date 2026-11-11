package com.esthetiadmin.backend.appointment.service;

import com.esthetiadmin.backend.availability.dto.DaySchedule;
import com.esthetiadmin.backend.availability.dto.TimeRange;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SlotCalculatorTest {

    private static final TimeRange MORNING = range("09:00", "11:00");

    @Test
    void halfHourAppointmentsCanStartEveryHalfHourUntilTheLastOneFits() {
        assertThat(times(SlotCalculator.freeStarts(schedule(List.of(MORNING), List.of()), 30, Set.of(), -1)))
                .containsExactly("09:00", "09:30", "10:00", "10:30");
    }

    @Test
    void anHourLongAppointmentMustEndBeforeClosing() {
        assertThat(times(SlotCalculator.freeStarts(schedule(List.of(MORNING), List.of()), 60, Set.of(), -1)))
                .containsExactly("09:00", "09:30", "10:00");
    }

    @Test
    void aTakenSlotRemovesEveryStartThatWouldOverlapIt() {
        Set<Integer> taken = Set.of(minute("10:00"));

        assertThat(times(SlotCalculator.freeStarts(schedule(List.of(MORNING), List.of()), 60, taken, -1)))
                .containsExactly("09:00");
        assertThat(times(SlotCalculator.freeStarts(schedule(List.of(MORNING), List.of()), 30, taken, -1)))
                .containsExactly("09:00", "09:30", "10:30");
    }

    @Test
    void blockedRangesAndWholeDaysOfferNothingInside() {
        DaySchedule partly = schedule(List.of(MORNING), List.of(range("09:30", "10:00")));
        DaySchedule closed = schedule(List.of(MORNING), List.of(TimeRange.WHOLE_DAY));

        assertThat(times(SlotCalculator.freeStarts(partly, 30, Set.of(), -1)))
                .containsExactly("09:00", "10:00", "10:30");
        assertThat(SlotCalculator.freeStarts(closed, 30, Set.of(), -1)).isEmpty();
    }

    @Test
    void severalRangesInADayAreAllOffered() {
        DaySchedule day = schedule(List.of(range("09:00", "10:00"), range("15:00", "16:00")), List.of());

        assertThat(times(SlotCalculator.freeStarts(day, 60, Set.of(), -1))).containsExactly("09:00", "15:00");
    }

    @Test
    void aClosedDayOffersNothing() {
        assertThat(SlotCalculator.freeStarts(schedule(List.of(), List.of()), 30, Set.of(), -1)).isEmpty();
    }

    @Test
    void todayOnlyOffersWhatHasNotStartedYet() {
        LocalDate today = LocalDate.of(2026, 10, 8);
        int notBefore = SlotCalculator.notBefore(LocalDateTime.of(2026, 10, 8, 9, 30), today);

        assertThat(times(SlotCalculator.freeStarts(schedule(List.of(MORNING), List.of()), 30, Set.of(), notBefore)))
                .containsExactly("10:00", "10:30");
        assertThat(SlotCalculator.notBefore(LocalDateTime.of(2026, 10, 8, 9, 30), today.plusDays(1))).isEqualTo(-1);
        assertThat(SlotCalculator.notBefore(LocalDateTime.of(2026, 10, 8, 9, 30), today.minusDays(1)))
                .isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void theLastHalfHourOfTheDayDoesNotWrapAroundMidnight() {
        DaySchedule late = schedule(List.of(range("22:00", "23:30")), List.of());

        assertThat(times(SlotCalculator.freeStarts(late, 60, Set.of(), -1))).containsExactly("22:00", "22:30");
    }

    @Test
    void anAppointmentHoldsOneSlotPerHalfHour() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 8, 10, 30);

        assertThat(SlotCalculator.slotsOf(start, 30)).containsExactly(start);
        assertThat(SlotCalculator.slotsOf(start, 60)).containsExactly(start, start.plusMinutes(30));
    }

    @Test
    void onlyHalfHoursAreOnTheGrid() {
        assertThat(SlotCalculator.onGrid(LocalDateTime.of(2026, 10, 8, 10, 30))).isTrue();
        assertThat(SlotCalculator.onGrid(LocalDateTime.of(2026, 10, 8, 10, 15))).isFalse();
        assertThat(SlotCalculator.onGrid(LocalDateTime.of(2026, 10, 8, 10, 30, 5))).isFalse();
    }

    private static DaySchedule schedule(List<TimeRange> open, List<TimeRange> blocked) {
        return new DaySchedule(open, blocked);
    }

    private static TimeRange range(String start, String end) {
        return new TimeRange(minute(start), minute(end));
    }

    private static int minute(String time) {
        LocalTime parsed = LocalTime.parse(time);
        return parsed.getHour() * 60 + parsed.getMinute();
    }

    private static List<String> times(List<LocalTime> starts) {
        return starts.stream().map(LocalTime::toString).toList();
    }
}
