package com.esthetiadmin.backend.availability.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Days taken out of the schedule, from {@code startDate} to {@code endDate} (both included): whole days when it
 * has no hours, otherwise only that range of a single day.
 */
@Entity
@Table(name = "availability_block")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(length = 150)
    private String reason;

    public AvailabilityBlock(LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime,
                             String reason) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    public boolean isWholeDay() {
        return startTime == null;
    }
}
