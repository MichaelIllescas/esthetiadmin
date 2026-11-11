package com.esthetiadmin.backend.notification.service;

import com.esthetiadmin.backend.common.exception.EmailNotSentException;
import com.esthetiadmin.backend.notification.config.NotificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/** Sends one HTML email through the SMTP server of the {@code .env}. Any failure becomes an {@link EmailNotSentException}. */
@Component
@RequiredArgsConstructor
public class EmailSender {

    /** The templates show it as {@code cid:esthetiadminLogo}. */
    static final String LOGO_ID = "esthetiadminLogo";
    private static final ClassPathResource LOGO = new ClassPathResource("templates/mail/images/esthetiadmin-logo.png");

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public void send(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            // Embedded instead of linked: it shows without the client allowing external images.
            helper.addInline(LOGO_ID, LOGO, "image/png");
            mailSender.send(message);
        } catch (MailException | MessagingException ex) {
            throw new EmailNotSentException(ex);
        }
    }
}
