package com.esthetiadmin.backend.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Entity
@Table(name = "category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    /** The only appointment lengths the schedule supports, on its 30-minute grid (ADR 013). */
    public static final Set<Integer> APPOINTMENT_DURATIONS = Set.of(30, 60);
    public static final int DEFAULT_APPOINTMENT_MINUTES = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "appointment_minutes", nullable = false)
    private int appointmentMinutes = DEFAULT_APPOINTMENT_MINUTES;

    public Category(String name, int appointmentMinutes) {
        update(name, appointmentMinutes);
    }

    public final void update(String name, int appointmentMinutes) {
        this.name = name;
        this.appointmentMinutes = appointmentMinutes;
    }

    public void changeActive(boolean active) {
        this.active = active;
    }
}
