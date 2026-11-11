package com.esthetiadmin.backend.availability.repository;

import java.time.LocalDateTime;

/** An appointment as the native query of {@link AvailabilityBlockRepository} returns it. */
public interface AffectedAppointmentView {

    Long getId();

    LocalDateTime getStartsAt();

    String getFirstName();

    String getLastName();
}
