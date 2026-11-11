package com.esthetiadmin.backend.notification.service;

import com.esthetiadmin.backend.notification.config.NotificationProperties;
import com.esthetiadmin.backend.notification.dto.AppointmentMail;
import com.esthetiadmin.backend.notification.dto.NoticeType;
import com.esthetiadmin.backend.settings.dto.BusinessContact;
import com.esthetiadmin.backend.settings.service.SettingsLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentNotifierTest {

    private static final AppointmentMail MAIL = new AppointmentMail("<b>Carina</b>", "Diaz", "3415551234",
            "carina@mail.com", "Bozo", LocalDateTime.of(2026, 10, 12, 10, 30));

    private EmailSender sender;
    private SettingsLookup settings;

    @BeforeEach
    void setUp() {
        sender = mock(EmailSender.class);
        settings = mock(SettingsLookup.class);
    }

    @Test
    void theClientNoticeHasTheAppointmentAndTheWhatsappOfTheCenter() {
        when(settings.contact()).thenReturn(new BusinessContact("esthetiadmin@mail.com", "3415559876"));

        notifier(true).notifyClient(52L, MAIL, NoticeType.BOOKED);

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("carina@mail.com"), eq("Tu turno está confirmado"), html.capture());
        assertThat(html.getValue())
                .contains("Bozo", "lunes 12 de octubre", "10:30", "3415559876", "https://wa.me/5493415559876")
                .contains("&lt;b&gt;Carina&lt;/b&gt;")
                .doesNotContain("<b>Carina</b>");
    }

    @Test
    void withoutContactPhoneTheNoticeHasNoContactLine() {
        when(settings.contact()).thenReturn(new BusinessContact(null, null));

        notifier(true).notifyClient(52L, MAIL, NoticeType.CANCELLED);

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(sender).send(any(), eq("Tu turno fue cancelado"), html.capture());
        assertThat(html.getValue()).doesNotContain("WhatsApp");
    }

    @Test
    void theOwnerGetsTheNewBookingAtHerNotificationEmail() {
        when(settings.contact()).thenReturn(new BusinessContact("esthetiadmin@mail.com", null));

        notifier(true).notifyOwner(52L, MAIL);

        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("esthetiadmin@mail.com"), eq("Nuevo turno: <b>Carina</b> Diaz - 12/10 10:30"), html.capture());
        assertThat(html.getValue()).contains("3415551234", "carina@mail.com", "http://panel");
    }

    @Test
    void nothingIsSentWithoutNotificationEmailOrWithEmailsDisabled() {
        when(settings.contact()).thenReturn(new BusinessContact(null, null));
        notifier(true).notifyOwner(52L, MAIL);

        when(settings.contact()).thenReturn(new BusinessContact("esthetiadmin@mail.com", null));
        notifier(false).notifyOwner(52L, MAIL);
        notifier(false).notifyClient(52L, MAIL, NoticeType.BOOKED);

        verify(sender, never()).send(any(), any(), any());
    }

    @Test
    void whatsappNeedsTheCountryCodeOfLocalMobiles() {
        assertThat(AppointmentNotifier.whatsappUrl("3415559876")).isEqualTo("https://wa.me/5493415559876");
        assertThat(AppointmentNotifier.whatsappUrl("5493415559876")).isEqualTo("https://wa.me/5493415559876");
        assertThat(AppointmentNotifier.whatsappUrl(null)).isNull();
    }

    private AppointmentNotifier notifier(boolean enabled) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return new AppointmentNotifier(sender, engine, settings,
                new NotificationProperties(enabled, "turnos@mail.com", "http://panel"));
    }
}
