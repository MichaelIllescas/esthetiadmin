package com.esthetiadmin.backend.notification.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Emails of public bookings go out in another thread, so the person does not wait for the mail server. */
@Configuration
@EnableAsync
@EnableConfigurationProperties(NotificationProperties.class)
public class NotificationConfig {
}
