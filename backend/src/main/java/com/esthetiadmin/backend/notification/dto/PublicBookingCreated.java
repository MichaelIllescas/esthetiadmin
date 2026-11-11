package com.esthetiadmin.backend.notification.dto;

/** Published when a public booking is saved; its emails go out once the transaction commits. */
public record PublicBookingCreated(Long appointmentId, AppointmentMail mail) {
}
