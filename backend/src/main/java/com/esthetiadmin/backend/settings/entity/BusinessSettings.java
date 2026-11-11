package com.esthetiadmin.backend.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The business settings (ADR 016): a single row with {@code id = 1}, created empty by the migration. */
@Entity
@Table(name = "business_settings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessSettings {

    public static final long SINGLE_ROW_ID = 1L;

    @Id
    private Long id;

    @Column(name = "notification_email", length = 254)
    private String notificationEmail;

    @Column(name = "contact_phone", length = 15)
    private String contactPhone;

    public void update(String notificationEmail, String contactPhone) {
        this.notificationEmail = notificationEmail;
        this.contactPhone = contactPhone;
    }
}
