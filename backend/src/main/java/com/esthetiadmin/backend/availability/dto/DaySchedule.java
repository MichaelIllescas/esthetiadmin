package com.esthetiadmin.backend.availability.dto;

import java.util.List;

/** One date of the schedule: the opening ranges of its weekday and the blocked ranges of that date. */
public record DaySchedule(List<TimeRange> open, List<TimeRange> blocked) {
}
