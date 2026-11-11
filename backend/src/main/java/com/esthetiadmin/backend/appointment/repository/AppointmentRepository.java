package com.esthetiadmin.backend.appointment.repository;

import com.esthetiadmin.backend.appointment.entity.Appointment;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(
            LocalDateTime from, LocalDateTime to);

    List<Appointment> findAllByStartsAtGreaterThanEqualAndStartsAtLessThanAndStatusOrderByStartsAtAsc(
            LocalDateTime from, LocalDateTime to, AppointmentStatus status);

    /** Upcoming appointments of a phone, to cap what one person can book from the public page. */
    long countByPhoneAndStatusAndStartsAtAfter(String phone, AppointmentStatus status, LocalDateTime after);
}
