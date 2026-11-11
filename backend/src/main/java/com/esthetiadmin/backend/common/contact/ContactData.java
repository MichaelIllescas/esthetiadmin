package com.esthetiadmin.backend.common.contact;

import com.esthetiadmin.backend.common.exception.BadRequestException;

import java.util.Locale;

/**
 * How phones and emails are kept. Phones are compared and stored as digits only, so "341 555-1234" and "3415551234"
 * are the same number. Emails are trimmed and in lower case; their format is checked by the DTOs.
 */
public final class ContactData {

    private static final int MIN_DIGITS = 8;
    private static final int MAX_DIGITS = 15;

    private ContactData() {
    }

    public static String phone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BadRequestException("El teléfono es obligatorio");
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < MIN_DIGITS || digits.length() > MAX_DIGITS) {
            throw new BadRequestException("El teléfono tiene que tener entre " + MIN_DIGITS + " y " + MAX_DIGITS
                    + " números");
        }
        return digits;
    }

    /** Like {@link #phone(String)}, but a missing phone is valid and comes back as {@code null}. */
    public static String optionalPhone(String phone) {
        return phone == null || phone.isBlank() ? null : phone(phone);
    }

    /** A blank email is the same as none. */
    public static String email(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
