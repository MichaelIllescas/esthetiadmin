package com.esthetiadmin.backend.appointment.service;

import com.esthetiadmin.backend.appointment.dto.AvailableDaysResponse;
import com.esthetiadmin.backend.appointment.dto.DayAvailabilityResponse;
import com.esthetiadmin.backend.appointment.dto.PublicBookingRequest;
import com.esthetiadmin.backend.appointment.dto.PublicBookingResponse;
import com.esthetiadmin.backend.appointment.entity.Appointment;
import com.esthetiadmin.backend.appointment.entity.AppointmentSource;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.esthetiadmin.backend.appointment.repository.AppointmentRepository;
import com.esthetiadmin.backend.availability.dto.DaySchedule;
import com.esthetiadmin.backend.availability.service.AvailabilityLookup;
import com.esthetiadmin.backend.catalog.dto.BookableCategory;
import com.esthetiadmin.backend.catalog.dto.OfferingInfo;
import com.esthetiadmin.backend.catalog.service.CatalogLookup;
import com.esthetiadmin.backend.common.contact.ContactData;
import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.common.exception.ConflictException;
import com.esthetiadmin.backend.common.exception.NotFoundException;
import com.esthetiadmin.backend.common.exception.TooManyRequestsException;
import com.esthetiadmin.backend.common.ratelimit.RateLimitConfig;
import com.esthetiadmin.backend.common.ratelimit.RateLimiter;
import com.esthetiadmin.backend.notification.dto.AppointmentMail;
import com.esthetiadmin.backend.notification.dto.PublicBookingCreated;
import com.esthetiadmin.backend.notification.service.AppointmentNotifier;
import com.esthetiadmin.backend.settings.service.SettingsLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the public booking page can do without login (ADR 014): see the bookable services and their free times, and
 * book one. It never returns prices nor anybody else's data. Stricter than the panel: only free times inside the
 * opening hours, up to 60 days ahead, at most 2 upcoming appointments per phone and 5 requests per hour per IP.
 */
@Service
public class PublicBookingService {

    private static final Logger log = LoggerFactory.getLogger(PublicBookingService.class);
    static final int DAYS_AHEAD = 60;
    static final int MAX_UPCOMING_PER_PHONE = 2;
    private static final String TAKEN = "Ese horario acaba de ocuparse, elegí otro";
    private static final String NOT_BOOKABLE = "El servicio no existe o no se puede reservar online";

    private final AppointmentRepository appointmentRepository;
    private final AppointmentSlots slots;
    private final CatalogLookup catalogLookup;
    private final AvailabilityLookup availabilityLookup;
    private final Clock clock;
    private final RateLimiter bookingLimiter;
    private final SettingsLookup settingsLookup;
    private final AppointmentNotifier notifier;
    private final ApplicationEventPublisher events;

    public PublicBookingService(AppointmentRepository appointmentRepository, AppointmentSlots slots,
                                CatalogLookup catalogLookup, AvailabilityLookup availabilityLookup, Clock clock,
                                @Qualifier(RateLimitConfig.PUBLIC_BOOKING) RateLimiter bookingLimiter,
                                SettingsLookup settingsLookup, AppointmentNotifier notifier,
                                ApplicationEventPublisher events) {
        this.appointmentRepository = appointmentRepository;
        this.slots = slots;
        this.catalogLookup = catalogLookup;
        this.availabilityLookup = availabilityLookup;
        this.clock = clock;
        this.bookingLimiter = bookingLimiter;
        this.settingsLookup = settingsLookup;
        this.notifier = notifier;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<BookableCategory> services() {
        return catalogLookup.bookableCatalog();
    }

    /** The days of {@code month} inside the booking window that still have a free time for the service. */
    @Transactional(readOnly = true)
    public AvailableDaysResponse availableDays(Long serviceId, YearMonth month) {
        OfferingInfo offering = getBookable(serviceId);
        LocalDate today = today();
        LocalDate from = max(month.atDay(1), today);
        LocalDate to = min(month.atEndOfMonth(), lastBookableDay());
        if (from.isAfter(to)) {
            return new AvailableDaysResponse(month, List.of());
        }
        Map<LocalDate, DaySchedule> schedule = availabilityLookup.scheduleBetween(from, to);
        Map<LocalDate, Set<Integer>> taken = slots.takenMinutesBetween(from, to, null);
        LocalDateTime now = LocalDateTime.now(clock);
        List<LocalDate> days = schedule.entrySet().stream()
                .filter(day -> !SlotCalculator.freeStarts(day.getValue(), offering.appointmentMinutes(),
                        taken.getOrDefault(day.getKey(), Set.of()), SlotCalculator.notBefore(now, day.getKey()))
                        .isEmpty())
                .map(Map.Entry::getKey)
                .toList();
        return new AvailableDaysResponse(month, days);
    }

    @Transactional(readOnly = true)
    public DayAvailabilityResponse availability(Long serviceId, LocalDate date) {
        OfferingInfo offering = getBookable(serviceId);
        ensureInWindow(date);
        return new DayAvailabilityResponse(date, offering.appointmentMinutes(),
                SlotCalculator.formatted(freeStarts(offering, date)));
    }

    /**
     * The appointment is confirmed right away (ADR 014). {@code clientIp} counts against the hourly limit. Its emails
     * (to the client, if she gave one, and to the owner) go out after the commit (ADR 016).
     */
    @Transactional
    public PublicBookingResponse book(PublicBookingRequest request, String clientIp) {
        if (!bookingLimiter.tryAcquire(clientIp)) {
            log.warn("Public booking rejected: too many requests from one address");
            throw new TooManyRequestsException("Hiciste demasiados pedidos de turno. Probá de nuevo más tarde");
        }
        OfferingInfo offering = getBookable(request.serviceId());
        LocalDateTime startsAt = request.startsAt();
        ensureInWindow(startsAt.toLocalDate());
        if (!SlotCalculator.onGrid(startsAt)
                || !freeStarts(offering, startsAt.toLocalDate()).contains(startsAt.toLocalTime())) {
            throw new ConflictException(TAKEN);
        }
        String phone = ContactData.phone(request.phone());
        if (appointmentRepository.countByPhoneAndStatusAndStartsAtAfter(phone, AppointmentStatus.CONFIRMED,
                LocalDateTime.now(clock)) >= MAX_UPCOMING_PER_PHONE) {
            String contactPhone = settingsLookup.contact().contactPhone();
            throw new ConflictException("Ya tenés " + MAX_UPCOMING_PER_PHONE
                    + " turnos reservados. Para otro, comunicate con el centro"
                    + (contactPhone == null ? "" : " al " + contactPhone));
        }

        Appointment appointment = new Appointment(AppointmentSource.PUBLIC, LocalDateTime.now(clock));
        appointment.schedule(offering.id(), startsAt, startsAt.plusMinutes(offering.appointmentMinutes()));
        String email = ContactData.email(request.email());
        appointment.changeContact(null, request.firstName().trim(), request.lastName().trim(), phone, email, null);
        Appointment saved = appointmentRepository.save(appointment);
        slots.hold(saved, offering.appointmentMinutes(), TAKEN);
        log.info("Appointment created: appointmentId={}, serviceId={}, startsAt={}, source=PUBLIC", saved.getId(),
                saved.getServiceId(), saved.getStartsAt());
        events.publishEvent(new PublicBookingCreated(saved.getId(), new AppointmentMail(saved.getFirstName(),
                saved.getLastName(), phone, email, offering.name(), saved.getStartsAt())));
        return new PublicBookingResponse(offering.name(), saved.getStartsAt(), saved.getEndsAt(),
                saved.getFirstName(), email != null && notifier.enabled());
    }

    private List<LocalTime> freeStarts(OfferingInfo offering, LocalDate date) {
        return SlotCalculator.freeStarts(availabilityLookup.scheduleOf(date), offering.appointmentMinutes(),
                slots.takenMinutes(date, null), SlotCalculator.notBefore(LocalDateTime.now(clock), date));
    }

    /** A service that is inactive, of an inactive category or not bookable looks the same as one that does not exist. */
    private OfferingInfo getBookable(Long serviceId) {
        return catalogLookup.findOffering(serviceId)
                .filter(offering -> offering.offerable() && offering.bookable())
                .orElseThrow(() -> new NotFoundException(NOT_BOOKABLE));
    }

    private void ensureInWindow(LocalDate date) {
        if (date.isBefore(today()) || date.isAfter(lastBookableDay())) {
            throw new BadRequestException("Elegí un día entre hoy y los próximos " + DAYS_AHEAD + " días");
        }
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDate lastBookableDay() {
        return today().plusDays(DAYS_AHEAD);
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
