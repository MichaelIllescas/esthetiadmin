package com.esthetiadmin.backend.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** A 30-minute slot held by an active appointment. Its start is unique, so two appointments can never overlap. */
@Entity
@Table(name = "appointment_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppointmentSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false, updatable = false)
    private Long appointmentId;

    @Column(name = "slot_start", nullable = false, unique = true, updatable = false)
    private LocalDateTime slotStart;

    public AppointmentSlot(Long appointmentId, LocalDateTime slotStart) {
        this.appointmentId = appointmentId;
        this.slotStart = slotStart;
    }
}
