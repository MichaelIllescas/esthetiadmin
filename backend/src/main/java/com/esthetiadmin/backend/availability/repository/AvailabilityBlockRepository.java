package com.esthetiadmin.backend.availability.repository;

import com.esthetiadmin.backend.availability.entity.AvailabilityBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface AvailabilityBlockRepository extends JpaRepository<AvailabilityBlock, Long> {

    /** Blocks that have not finished by {@code from}. */
    List<AvailabilityBlock> findAllByEndDateGreaterThanEqualOrderByStartDateAscStartTimeAsc(LocalDate from);

    /** Blocks that share at least one day with {@code from}..{@code to}. */
    List<AvailabilityBlock> findAllByEndDateGreaterThanEqualAndStartDateLessThanEqualOrderByStartDateAscStartTimeAsc(
            LocalDate from, LocalDate to);

    /**
     * Confirmed appointments that overlap {@code from}..{@code to}. Native on purpose: the schedule must not depend on
     * the appointment module, which depends on it.
     */
    @Query(value = """
            SELECT id, starts_at AS startsAt, first_name AS firstName, last_name AS lastName
            FROM appointment
            WHERE status = 'CONFIRMED' AND starts_at < :to AND ends_at > :from
            ORDER BY starts_at""", nativeQuery = true)
    List<AffectedAppointmentView> findConfirmedAppointmentsBetween(@Param("from") LocalDateTime from,
                                                                   @Param("to") LocalDateTime to);
}
