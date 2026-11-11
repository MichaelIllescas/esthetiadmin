package com.esthetiadmin.backend.settings.dto;

/** What other modules may know of the business: where the owner gets notices and the phone clients call. */
public record BusinessContact(String notificationEmail, String contactPhone) {
}
