package com.esthetiadmin.backend.appointment.service;

import com.esthetiadmin.backend.appointment.dto.AppointmentRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentResponse;
import com.esthetiadmin.backend.appointment.dto.DayAvailabilityResponse;
import com.esthetiadmin.backend.appointment.entity.Appointment;
import com.esthetiadmin.backend.appointment.entity.AppointmentSource;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.esthetiadmin.backend.appointment.repository.AppointmentRepository;
import com.esthetiadmin.backend.availability.service.AvailabilityLookup;
import com.esthetiadmin.backend.catalog.dto.OfferingInfo;
import com.esthetiadmin.backend.catalog.service.CatalogLookup;
import com.esthetiadmin.backend.client.dto.ClientContact;
import com.esthetiadmin.backend.client.service.ClientLookup;
import com.esthetiadmin.backend.common.contact.ContactData;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import com.esthetiadmin.backend.notification.dto.AppointmentMail;
import com.esthetiadmin.backend.notification.dto.NoticeType;
import com.esthetiadmin.backend.notification.service.AppointmentNotifier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/** The owner's agenda: appointments loaded from the panel, their changes and their outcome (ADR 013). */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);
    private static final int MAX_RANGE_DAYS = 62;
    private static final String TAKEN = "Ese horario ya está ocupado";
    private static final String UNKNOWN = "(desconocido)";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlots slots;
    private final CatalogLookup catalogLookup;
    private final ClientLookup clientLookup;
    private final AvailabilityLookup availabilityLookup;
    private final AppointmentNotifier notifier;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AppointmentResponse> search(LocalDate from, LocalDate to, AppointmentStatus status) {
        if (from.isAfter(to)) {
            throw new BadRequestException("La fecha \"desde\" no puede ser posterior a la fecha \"hasta\"");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BadRequestException("El rango de fechas no puede superar los " + MAX_RANGE_DAYS + " días");
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<Appointment> appointments = status == null
                ? appointmentRepository.findAllByStartsAtGreaterThanEqualAndStartsAtLessThanOrderByStartsAtAsc(start, end)
                : appointmentRepository.findAllByStartsAtGreaterThanEqualAndStartsAtLessThanAndStatusOrderByStartsAtAsc(
                        start, end, status);
        return toResponses(appointments);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id) {
        return toResponse(getAppointment(id));
    }

    /**
     * Free start times of a day for a service, as the owner sees them: opening hours, blocks and taken slots apply,
     * without the 60-day limit of the public page. {@code excludeAppointmentId} frees that appointment's own time,
     * for when it is being moved.
     */
    @Transactional(readOnly = true)
    public DayAvailabilityResponse availability(Long serviceId, LocalDate date, Long excludeAppointmentId) {
        OfferingInfo offering = getOfferable(serviceId);
        int duration = offering.appointmentMinutes();
        List<String> times = SlotCalculator.formatted(SlotCalculator.freeStarts(availabilityLookup.scheduleOf(date),
                duration, slots.takenMinutes(date, excludeAppointmentId),
                SlotCalculator.notBefore(LocalDateTime.now(clock), date)));
        return new DayAvailabilityResponse(date, duration, times);
    }

    /** From the panel it may fall outside the opening hours or a block: she decides her agenda. Never overlapping. */
    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        Appointment appointment = new Appointment(AppointmentSource.ADMIN, LocalDateTime.now(clock));
        applyContact(appointment, request);
        OfferingInfo offering = applySchedule(appointment, request);
        Appointment saved = appointmentRepository.save(appointment);
        slots.hold(saved, offering.appointmentMinutes(), TAKEN);
        shareEmailWithClient(saved);
        log.info("Appointment created: appointmentId={}, serviceId={}, startsAt={}, source=ADMIN", saved.getId(),
                saved.getServiceId(), saved.getStartsAt());
        return toResponse(saved);
    }

    /** Moves or corrects a confirmed appointment; its previous time is freed. */
    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request) {
        Appointment appointment = getAppointment(id);
        ensureConfirmed(appointment, "Solo se puede modificar un turno confirmado");
        applyContact(appointment, request);
        OfferingInfo offering = applySchedule(appointment, request);
        slots.release(id);
        slots.hold(appointment, offering.appointmentMinutes(), TAKEN);
        shareEmailWithClient(appointment);
        log.info("Appointment updated: appointmentId={}, serviceId={}, startsAt={}", id, appointment.getServiceId(),
                appointment.getStartsAt());
        return toResponse(appointment);
    }

    /** Only from CONFIRMED. Cancelled and no-show free the time; attended needs the client it belongs to. */
    @Transactional
    public AppointmentResponse changeStatus(Long id, AppointmentStatus status) {
        Appointment appointment = getAppointment(id);
        if (status == AppointmentStatus.CONFIRMED) {
            throw new BadRequestException("Un turno no puede volver a quedar confirmado");
        }
        ensureConfirmed(appointment, "El turno ya no está confirmado");
        if (status == AppointmentStatus.ATTENDED && appointment.getClientId() == null) {
            throw new ConflictException("Vinculá el turno con un cliente antes de marcarlo atendido");
        }
        appointment.changeStatus(status);
        if (!status.holdsTime()) {
            slots.release(id);
        }
        log.info("Appointment status changed: appointmentId={}, status={}", id, status);
        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse linkClient(Long id, Long clientId) {
        Appointment appointment = getAppointment(id);
        ensureConfirmed(appointment, "Solo se puede vincular un turno confirmado");
        if (!clientLookup.exists(clientId)) {
            throw new NotFoundException("El cliente no existe");
        }
        appointment.linkClient(clientId);
        if (appointment.getEmail() == null) {
            clientLookup.findContact(clientId).map(ClientContact::email).ifPresent(appointment::changeEmail);
        }
        shareEmailWithClient(appointment);
        log.info("Appointment linked to client: appointmentId={}, clientId={}", id, clientId);
        return toResponse(appointment);
    }

    /**
     * Emails the client about her appointment, only when the owner asks for it (ADR 016). Not transactional: the
     * mail server may take a while and nothing is written.
     */
    public void sendNotice(Long id, NoticeType type) {
        Appointment appointment = getAppointment(id);
        if (appointment.getEmail() == null) {
            throw new BadRequestException("La clienta no tiene email cargado");
        }
        AppointmentStatus expected = type == NoticeType.CANCELLED
                ? AppointmentStatus.CANCELLED : AppointmentStatus.CONFIRMED;
        if (appointment.getStatus() != expected) {
            throw new ConflictException("Ese aviso no corresponde al estado del turno");
        }
        String serviceName = catalogLookup.findOffering(appointment.getServiceId())
                .map(OfferingInfo::name).orElse(UNKNOWN);
        notifier.notifyClient(id, new AppointmentMail(appointment.getFirstName(), appointment.getLastName(),
                appointment.getPhone(), appointment.getEmail(), serviceName, appointment.getStartsAt()), type);
    }

    /** The appointment's email goes to its client's file when she has none there (ADR 016). */
    private void shareEmailWithClient(Appointment appointment) {
        if (appointment.getClientId() != null && appointment.getEmail() != null) {
            clientLookup.fillMissingEmail(appointment.getClientId(), appointment.getEmail());
        }
    }

    private OfferingInfo applySchedule(Appointment appointment, AppointmentRequest request) {
        // Keeping its own service is always fine, even if that service was turned off after it was booked.
        OfferingInfo offering = request.serviceId().equals(appointment.getServiceId())
                ? catalogLookup.findOffering(request.serviceId())
                        .orElseThrow(() -> new NotFoundException("El servicio no existe"))
                : getOfferable(request.serviceId());
        LocalDateTime startsAt = request.startsAt();
        if (!SlotCalculator.onGrid(startsAt)) {
            throw new BadRequestException("El turno tiene que empezar en punto o y media");
        }
        if (!startsAt.isAfter(LocalDateTime.now(clock))) {
            throw new BadRequestException("No se puede dar un turno en un horario que ya pasó");
        }
        appointment.schedule(offering.id(), startsAt, startsAt.plusMinutes(offering.appointmentMinutes()));
        return offering;
    }

    /** Missing name, phone or email are taken from the linked client; the phone is kept as digits only. */
    private void applyContact(Appointment appointment, AppointmentRequest request) {
        ClientContact client = request.clientId() == null ? null : clientLookup.findContact(request.clientId())
                .orElseThrow(() -> new NotFoundException("El cliente no existe"));
        String firstName = firstNonBlank(request.firstName(), client == null ? null : client.firstName());
        String lastName = firstNonBlank(request.lastName(), client == null ? null : client.lastName());
        String phone = firstNonBlank(request.phone(), client == null ? null : client.phone());
        String email = firstNonBlank(request.email(), client == null ? null : client.email());
        if (firstName == null || lastName == null) {
            throw new BadRequestException("El nombre y el apellido son obligatorios");
        }
        appointment.changeContact(request.clientId(), firstName, lastName, ContactData.phone(phone),
                ContactData.email(email), firstNonBlank(request.notes(), null));
    }

    private OfferingInfo getOfferable(Long serviceId) {
        OfferingInfo offering = catalogLookup.findOffering(serviceId)
                .orElseThrow(() -> new NotFoundException("El servicio no existe"));
        if (!offering.offerable()) {
            throw new ConflictException("El servicio no está disponible: está desactivado o su categoría lo está");
        }
        return offering;
    }

    private void ensureConfirmed(Appointment appointment, String message) {
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new ConflictException(message);
        }
    }

    private Appointment getAppointment(Long id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new NotFoundException("El turno no existe"));
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return toResponses(List.of(appointment)).get(0);
    }

    private List<AppointmentResponse> toResponses(List<Appointment> appointments) {
        Map<Long, OfferingInfo> offerings = catalogLookup.findOfferings(
                appointments.stream().map(Appointment::getServiceId).distinct().toList());
        return appointments.stream().map(appointment -> {
            OfferingInfo offering = offerings.get(appointment.getServiceId());
            return new AppointmentResponse(appointment.getId(), appointment.getServiceId(),
                    offering == null ? UNKNOWN : offering.name(), offering == null ? UNKNOWN : offering.categoryName(),
                    appointment.getStartsAt(), appointment.getEndsAt(), appointment.getFirstName(),
                    appointment.getLastName(), appointment.getPhone(), appointment.getClientId(),
                    appointment.getStatus(), appointment.getSource(), appointment.getNotes(), appointment.getEmail());
        }).toList();
    }

    private static String firstNonBlank(String value, String fallback) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return fallback == null || fallback.isBlank() ? null : fallback.trim();
    }
}
