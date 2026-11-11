package com.esthetiadmin.backend.notification.service;

import com.esthetiadmin.backend.notification.config.NotificationProperties;
import com.esthetiadmin.backend.notification.dto.AppointmentMail;
import com.esthetiadmin.backend.notification.dto.NoticeType;
import com.esthetiadmin.backend.settings.dto.BusinessContact;
import com.esthetiadmin.backend.settings.service.SettingsLookup;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Writes and sends the appointment emails (ADR 016): the notices to the client and the new-booking notice to the
 * owner. Logs never contain an email address. With emails disabled it only logs that it skipped.
 */
@Service
@RequiredArgsConstructor
public class AppointmentNotifier {

    private static final Logger log = LoggerFactory.getLogger(AppointmentNotifier.class);
    private static final Locale SPANISH = Locale.forLanguageTag("es-AR");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH);
    private static final DateTimeFormatter SHORT_DAY = DateTimeFormatter.ofPattern("dd/MM", SPANISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    /** Local Argentine mobiles are stored with 10 digits; WhatsApp needs them as 54 9 + those digits. */
    private static final int LOCAL_MOBILE_DIGITS = 10;

    private final EmailSender emailSender;
    private final ITemplateEngine templateEngine;
    private final SettingsLookup settingsLookup;
    private final NotificationProperties properties;

    public boolean enabled() {
        return properties.enabled();
    }

    /** Throws {@link com.esthetiadmin.backend.common.exception.EmailNotSentException} if the mail server fails. */
    public void notifyClient(Long appointmentId, AppointmentMail mail, NoticeType type) {
        if (!enabled()) {
            log.info("Email disabled, client notice skipped: appointmentId={}, type={}", appointmentId, type);
            return;
        }
        Context context = baseContext(mail);
        context.setVariable("headline", type.headline());
        context.setVariable("cancelled", type == NoticeType.CANCELLED);
        String phone = settingsLookup.contact().contactPhone();
        context.setVariable("contactPhone", phone);
        context.setVariable("whatsappUrl", whatsappUrl(phone));
        emailSender.send(mail.email(), type.subject(), templateEngine.process("mail/client-notice", context));
        log.info("Client notice sent: appointmentId={}, type={}", appointmentId, type);
    }

    /** Does nothing when the owner has no notification email in Configuración. */
    public void notifyOwner(Long appointmentId, AppointmentMail mail) {
        BusinessContact contact = settingsLookup.contact();
        if (!enabled() || contact.notificationEmail() == null) {
            log.info("Owner notice skipped: appointmentId={}, emailEnabled={}, notificationEmailSet={}",
                    appointmentId, enabled(), contact.notificationEmail() != null);
            return;
        }
        Context context = baseContext(mail);
        context.setVariable("lastName", mail.lastName());
        context.setVariable("phone", mail.phone());
        context.setVariable("clientEmail", mail.email());
        context.setVariable("panelUrl", properties.panelUrl());
        String subject = "Nuevo turno: " + mail.firstName() + " " + mail.lastName() + " - "
                + SHORT_DAY.format(mail.startsAt()) + " " + TIME.format(mail.startsAt());
        emailSender.send(contact.notificationEmail(), subject,
                templateEngine.process("mail/owner-new-booking", context));
        log.info("Owner notice sent: appointmentId={}", appointmentId);
    }

    private static Context baseContext(AppointmentMail mail) {
        Context context = new Context(SPANISH);
        context.setVariable("firstName", mail.firstName());
        context.setVariable("serviceName", mail.serviceName());
        context.setVariable("day", DAY.format(mail.startsAt()));
        context.setVariable("time", TIME.format(mail.startsAt()));
        return context;
    }

    static String whatsappUrl(String phone) {
        if (phone == null) {
            return null;
        }
        return "https://wa.me/" + (phone.length() == LOCAL_MOBILE_DIGITS ? "549" + phone : phone);
    }
}
