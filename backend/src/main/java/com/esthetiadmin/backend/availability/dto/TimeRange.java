package com.esthetiadmin.backend.availability.dto;

/** A range of one day in minutes since midnight, end excluded: 0..1440 is the whole day. */
public record TimeRange(int startMinute, int endMinute) {

    public static final TimeRange WHOLE_DAY = new TimeRange(0, 24 * 60);

    public boolean overlaps(int start, int end) {
        return start < endMinute && startMinute < end;
    }

    /** Whether {@code start}..{@code end} fits completely inside this range. */
    public boolean contains(int start, int end) {
        return startMinute <= start && end <= endMinute;
    }
}
