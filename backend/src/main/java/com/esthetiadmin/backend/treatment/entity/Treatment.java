package com.esthetiadmin.backend.treatment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A service performed on a client on a given day, with the amount actually charged. */
@Entity
@Table(name = "treatment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Treatment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false, updatable = false)
    private Long clientId;

    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    @Column(name = "performed_on", nullable = false)
    private LocalDate performedOn;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(columnDefinition = "text")
    private String notes;

    public Treatment(Long clientId, Long serviceId, LocalDate performedOn, BigDecimal amount, String notes) {
        this.clientId = clientId;
        update(serviceId, performedOn, amount, notes);
    }

    /** The client is not part of the update: a treatment never moves to another person. */
    public final void update(Long serviceId, LocalDate performedOn, BigDecimal amount, String notes) {
        this.serviceId = serviceId;
        this.performedOn = performedOn;
        this.amount = amount;
        this.notes = notes;
    }
}
