package com.esthetiadmin.backend.notification.service;

import com.esthetiadmin.backend.common.exception.EmailNotSentException;
import com.esthetiadmin.backend.notification.dto.AppointmentMail;
import com.esthetiadmin.backend.notification.dto.NoticeType;
import com.esthetiadmin.backend.notification.dto.PublicBookingCreated;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PublicBookingMailListenerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 12, 10, 30);

    private final AppointmentNotifier notifier = mock(AppointmentNotifier.class);
    private final PublicBookingMailListener listener = new PublicBookingMailListener(notifier);

    @Test
    void aFailedClientEmailDoesNotStopTheOwnerNotice() {
        AppointmentMail mail = new AppointmentMail("Carina", "Diaz", "3415551234", "carina@mail.com", "Bozo", START);
        doThrow(new EmailNotSentException(new RuntimeException("smtp down")))
                .when(notifier).notifyClient(52L, mail, NoticeType.BOOKED);

        listener.onPublicBooking(new PublicBookingCreated(52L, mail));

        verify(notifier).notifyOwner(52L, mail);
    }

    @Test
    void withoutEmailOnlyTheOwnerIsNotified() {
        AppointmentMail mail = new AppointmentMail("Carina", "Diaz", "3415551234", null, "Bozo", START);

        listener.onPublicBooking(new PublicBookingCreated(52L, mail));

        verify(notifier, never()).notifyClient(any(), any(), any());
        verify(notifier).notifyOwner(52L, mail);
    }
}
