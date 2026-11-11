package com.esthetiadmin.backend.appointment.entity;

/** Every appointment starts CONFIRMED; the other three are final. */
public enum AppointmentStatus {
    CONFIRMED,
    CANCELLED,
    ATTENDED,
    NO_SHOW;

    /** Whether the appointment still holds its time: a cancelled one or a no-show frees it. */
    public boolean holdsTime() {
        return this == CONFIRMED || this == ATTENDED;
    }
}
