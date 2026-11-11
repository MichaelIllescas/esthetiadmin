package com.esthetiadmin.backend.client.dto;

/** What other modules may know about a client to contact her: her name, phone and email (both may be missing). */
public record ClientContact(Long id, String firstName, String lastName, String phone, String email) {
}
