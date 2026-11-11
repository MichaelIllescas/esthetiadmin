package com.esthetiadmin.backend.notification.dto;

import java.time.LocalDateTime;

/** The data of an appointment an email needs. {@code email} is the client's and may be missing. */
public record AppointmentMail(String firstName, String lastName, String phone, String email, String serviceName,
                              LocalDateTime startsAt) {
}
