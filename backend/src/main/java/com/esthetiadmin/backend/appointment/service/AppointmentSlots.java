package com.esthetiadmin.backend.appointment.service;

import com.esthetiadmin.backend.appointment.entity.Appointment;
import com.esthetiadmin.backend.appointment.entity.AppointmentSlot;
import com.esthetiadmin.backend.appointment.repository.AppointmentSlotRepository;
import com.esthetiadmin.backend.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The slots appointments hold, shared by the agenda and the public booking so both follow the same rules. */
@Component
@RequiredArgsConstructor
public class AppointmentSlots {

    private final AppointmentSlotRepository slotRepository;

    /** Minutes since midnight of the taken slots of {@code date}, ignoring {@code excludeAppointmentId}'s own. */
    public Set<Integer> takenMinutes(LocalDate date, Long excludeAppointmentId) {
        return takenMinutesBetween(date, date, excludeAppointmentId).getOrDefault(date, Set.of());
    }

    /** The same for every day of {@code from}..{@code to}, in one query. */
    public Map<LocalDate, Set<Integer>> takenMinutesBetween(LocalDate from, LocalDate to, Long excludeAppointmentId) {
        Map<LocalDate, Set<Integer>> taken = new HashMap<>();
        for (LocalDateTime slot : slotRepository.findTakenStarts(from.atStartOfDay(), to.plusDays(1).atStartOfDay(),
                excludeAppointmentId)) {
            taken.computeIfAbsent(slot.toLocalDate(), day -> new HashSet<>()).add(slot.getHour() * 60 + slot.getMinute());
        }
        return taken;
    }

    /** Checks first for a clear message; the unique slot start still stops two requests racing for the same time. */
    public void hold(Appointment appointment, int durationMinutes, String takenMessage) {
        List<LocalDateTime> starts = SlotCalculator.slotsOf(appointment.getStartsAt(), durationMinutes);
        if (slotRepository.existsBySlotStartIn(starts)) {
            throw new ConflictException(takenMessage);
        }
        try {
            slotRepository.saveAllAndFlush(starts.stream()
                    .map(start -> new AppointmentSlot(appointment.getId(), start))
                    .toList());
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException(takenMessage);
        }
    }

    public void release(Long appointmentId) {
        slotRepository.deleteByAppointmentId(appointmentId);
    }
}
