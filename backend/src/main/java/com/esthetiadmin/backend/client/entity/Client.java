package com.esthetiadmin.backend.client.entity;

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

@Entity
@Table(name = "client")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 30)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(columnDefinition = "text")
    private String notes;

    public Client(String firstName, String lastName, String phone, LocalDate birthDate, String notes) {
        update(firstName, lastName, phone, birthDate, notes);
    }

    public final void update(String firstName, String lastName, String phone, LocalDate birthDate, String notes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.birthDate = birthDate;
        this.notes = notes;
    }

    /** Only for appointment notices (ADR 016); {@code null} when she gave none. */
    public void changeEmail(String email) {
        this.email = email;
    }

    /** An email she already has is never overwritten. */
    public boolean fillMissingEmail(String email) {
        if (this.email != null || email == null) {
            return false;
        }
        this.email = email;
        return true;
    }
}
