package com.esthetiadmin.backend.notification.service;

import com.esthetiadmin.backend.common.exception.EmailNotSentException;
import com.esthetiadmin.backend.notification.dto.NoticeType;
import com.esthetiadmin.backend.notification.dto.PublicBookingCreated;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails of a public booking (ADR 016): only after the booking is committed, in another thread, and a failure only
 * leaves a warning: the booking is already made.
 */
@Component
@RequiredArgsConstructor
public class PublicBookingMailListener {

    private static final Logger log = LoggerFactory.getLogger(PublicBookingMailListener.class);

    private final AppointmentNotifier notifier;

    @Async
    @TransactionalEventListener
    public void onPublicBooking(PublicBookingCreated event) {
        if (event.mail().email() != null) {
            try {
                notifier.notifyClient(event.appointmentId(), event.mail(), NoticeType.BOOKED);
            } catch (EmailNotSentException ex) {
                log.warn("Client notice of a public booking not sent: appointmentId={}", event.appointmentId(),
                        ex.getCause());
            }
        }
        try {
            notifier.notifyOwner(event.appointmentId(), event.mail());
        } catch (EmailNotSentException ex) {
            log.warn("Owner notice of a public booking not sent: appointmentId={}", event.appointmentId(),
                    ex.getCause());
        }
    }
}
