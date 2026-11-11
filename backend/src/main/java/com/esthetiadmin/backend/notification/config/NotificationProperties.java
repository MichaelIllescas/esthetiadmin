package com.esthetiadmin.backend.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Appointment emails (ADR 016). With {@code enabled = false} nothing is sent (development and tests).
 * {@code panelUrl} is the address of the panel, linked from the owner's email.
 */
@ConfigurationProperties(prefix = "app.notifications.email")
public record NotificationProperties(boolean enabled, String from, String panelUrl) {
}
