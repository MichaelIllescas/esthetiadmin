package com.esthetiadmin.backend.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A booking of a service at a time (ADR 013). It is not a treatment: it has no amount and does not count as income.
 * The person's name and phone are kept here; {@code clientId} is set once she is linked to a client.
 */
@Entity
@Table(name = "appointment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 15)
    private String phone;

    /** Optional, only for notices about this appointment (ADR 016). */
    @Column(length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6, updatable = false)
    private AppointmentSource source;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Appointment(AppointmentSource source, LocalDateTime createdAt) {
        this.source = source;
        this.createdAt = createdAt;
    }

    public void schedule(Long serviceId, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.serviceId = serviceId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public void changeContact(Long clientId, String firstName, String lastName, String phone, String email,
                              String notes) {
        this.clientId = clientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.email = email;
        this.notes = notes;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void linkClient(Long clientId) {
        this.clientId = clientId;
    }

    public void changeStatus(AppointmentStatus status) {
        this.status = status;
    }
}
