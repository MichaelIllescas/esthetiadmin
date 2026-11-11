package com.esthetiadmin.backend.availability.service;

import com.esthetiadmin.backend.availability.dto.DaySchedule;
import com.esthetiadmin.backend.availability.dto.TimeRange;
import com.esthetiadmin.backend.availability.entity.AvailabilityBlock;
import com.esthetiadmin.backend.availability.entity.AvailabilityRule;
import com.esthetiadmin.backend.availability.repository.AvailabilityBlockRepository;
import com.esthetiadmin.backend.availability.repository.AvailabilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The read-only door of the schedule for the rest of the modules, which must not reach its repositories. */
@Service
@RequiredArgsConstructor
public class AvailabilityLookup {

    private final AvailabilityRuleRepository ruleRepository;
    private final AvailabilityBlockRepository blockRepository;

    @Transactional(readOnly = true)
    public DaySchedule scheduleOf(LocalDate date) {
        return scheduleBetween(date, date).get(date);
    }

    /** Every day from {@code from} to {@code to} (both included), reading the rules and the blocks only once. */
    @Transactional(readOnly = true)
    public Map<LocalDate, DaySchedule> scheduleBetween(LocalDate from, LocalDate to) {
        List<AvailabilityRule> rules = ruleRepository.findAllByOrderByDayOfWeekAscStartTimeAsc();
        List<AvailabilityBlock> blocks = blockRepository
                .findAllByEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAscStartTimeAsc(from, to);
        Map<LocalDate, DaySchedule> days = new LinkedHashMap<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            LocalDate day = date;
            List<TimeRange> open = rules.stream()
                    .filter(rule -> rule.getDayOfWeek() == day.getDayOfWeek().getValue())
                    .map(rule -> new TimeRange(minuteOf(rule.getStartTime()), minuteOf(rule.getEndTime())))
                    .toList();
            List<TimeRange> blocked = blocks.stream()
                    .filter(block -> !day.isBefore(block.getStartDate()) && !day.isAfter(block.getEndDate()))
                    .map(AvailabilityLookup::rangeOf)
                    .toList();
            days.put(day, new DaySchedule(open, blocked));
        }
        return days;
    }

    private static TimeRange rangeOf(AvailabilityBlock block) {
        return block.isWholeDay()
                ? TimeRange.WHOLE_DAY
                : new TimeRange(minuteOf(block.getStartTime()), minuteOf(block.getEndTime()));
    }

    private static int minuteOf(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
