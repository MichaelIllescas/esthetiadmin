package com.esthetiadmin.backend.notification.dto;

/** The notices a client can get about her appointment, with the subject and the first line of the email. */
public enum NoticeType {
    BOOKED("Tu turno está confirmado", "Tu turno quedó confirmado."),
    RESCHEDULED("Tu turno cambió", "Tu turno fue reprogramado. Estos son los nuevos datos:"),
    CANCELLED("Tu turno fue cancelado", "Tu turno fue cancelado.");

    private final String subject;
    private final String headline;

    NoticeType(String subject, String headline) {
        this.subject = subject;
        this.headline = headline;
    }

    public String subject() {
        return subject;
    }

    public String headline() {
        return headline;
    }
}
