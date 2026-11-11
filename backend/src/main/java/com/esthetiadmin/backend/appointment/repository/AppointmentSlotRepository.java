package com.esthetiadmin.backend.appointment.repository;

import com.esthetiadmin.backend.appointment.entity.AppointmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    /** Taken slots of {@code from}..{@code to} (end excluded), leaving out the ones of {@code excludeAppointmentId}. */
    @Query("""
            SELECT s.slotStart FROM AppointmentSlot s
            WHERE s.slotStart >= :from AND s.slotStart < :to
              AND (:excludeAppointmentId IS NULL OR s.appointmentId <> :excludeAppointmentId)""")
    List<LocalDateTime> findTakenStarts(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                        @Param("excludeAppointmentId") Long excludeAppointmentId);

    boolean existsBySlotStartIn(Collection<LocalDateTime> starts);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM AppointmentSlot s WHERE s.appointmentId = :appointmentId")
    void deleteByAppointmentId(@Param("appointmentId") Long appointmentId);
}
