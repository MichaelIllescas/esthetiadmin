package com.esthetiadmin.backend.appointment.service;

import com.esthetiadmin.backend.availability.dto.DaySchedule;
import com.esthetiadmin.backend.availability.dto.TimeRange;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Works out the free start times of one day (ADR 013): on the 30-minute grid, inside an opening range and ending
 * before it closes, not touching a blocked range nor a taken slot, and not in the past. Pure, so it is easy to test.
 */
public final class SlotCalculator {

    public static final int GRID_MINUTES = 30;

    private SlotCalculator() {
    }

    /**
     * @param takenSlots minutes since midnight of the slots already held by other appointments
     * @param notBefore  the first minute that is not in the past: -1 for a future day, the current minute for today
     */
    public static List<LocalTime> freeStarts(DaySchedule schedule, int durationMinutes, Set<Integer> takenSlots,
                                             int notBefore) {
        List<LocalTime> starts = new ArrayList<>();
        for (TimeRange open : schedule.open()) {
            int first = roundUpToGrid(open.startMinute());
            for (int start = first; start + durationMinutes <= open.endMinute(); start += GRID_MINUTES) {
                int end = start + durationMinutes;
                if (start >= notBefore && !blocked(schedule, start, end) && !taken(takenSlots, start, end)) {
                    starts.add(LocalTime.of(start / 60, start % 60));
                }
            }
        }
        return starts.stream().distinct().sorted().toList();
    }

    /**
     * The first minute of {@code date} that is not in the past: -1 for a future date, none for a past one and, for
     * today, the minute after {@code now} (a start at the current minute has already begun).
     */
    public static int notBefore(LocalDateTime now, LocalDate date) {
        if (date.isBefore(now.toLocalDate())) {
            return Integer.MAX_VALUE;
        }
        return date.isAfter(now.toLocalDate()) ? -1 : now.getHour() * 60 + now.getMinute() + 1;
    }

    /** "09:00", "09:30"... as the API returns them. */
    public static List<String> formatted(List<LocalTime> starts) {
        return starts.stream().map(time -> String.format("%02d:%02d", time.getHour(), time.getMinute())).toList();
    }

    /** The slot starts an appointment of {@code durationMinutes} holds from {@code startsAt}. */
    public static List<LocalDateTime> slotsOf(LocalDateTime startsAt, int durationMinutes) {
        List<LocalDateTime> slots = new ArrayList<>();
        for (int offset = 0; offset < durationMinutes; offset += GRID_MINUTES) {
            slots.add(startsAt.plusMinutes(offset));
        }
        return slots;
    }

    public static boolean onGrid(LocalDateTime time) {
        return time.getSecond() == 0 && time.getNano() == 0 && time.getMinute() % GRID_MINUTES == 0;
    }

    private static int roundUpToGrid(int minute) {
        return (minute + GRID_MINUTES - 1) / GRID_MINUTES * GRID_MINUTES;
    }

    private static boolean blocked(DaySchedule schedule, int start, int end) {
        return schedule.blocked().stream().anyMatch(range -> range.overlaps(start, end));
    }

    private static boolean taken(Set<Integer> takenSlots, int start, int end) {
        for (int slot = start; slot < end; slot += GRID_MINUTES) {
            if (takenSlots.contains(slot)) {
                return true;
            }
        }
        return false;
    }
}
