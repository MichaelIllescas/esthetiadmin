package com.esthetiadmin.backend.settings.service;

import com.esthetiadmin.backend.common.contact.ContactData;
import com.esthetiadmin.backend.settings.dto.PublicContactResponse;
import com.esthetiadmin.backend.settings.dto.SettingsRequest;
import com.esthetiadmin.backend.settings.dto.SettingsResponse;
import com.esthetiadmin.backend.settings.entity.BusinessSettings;
import com.esthetiadmin.backend.settings.repository.BusinessSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The business contact data the owner edits in Configuración (ADR 016). */
@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final BusinessSettingsRepository repository;

    @Transactional(readOnly = true)
    public SettingsResponse get() {
        return SettingsResponse.from(load());
    }

    @Transactional
    public SettingsResponse update(SettingsRequest request) {
        BusinessSettings settings = load();
        settings.update(ContactData.email(request.notificationEmail()),
                ContactData.optionalPhone(request.contactPhone()));
        log.info("Business settings updated: notificationEmailSet={}, contactPhoneSet={}",
                settings.getNotificationEmail() != null, settings.getContactPhone() != null);
        return SettingsResponse.from(settings);
    }

    /** Only the phone: the notification email is never public. */
    @Transactional(readOnly = true)
    public PublicContactResponse publicContact() {
        return new PublicContactResponse(load().getContactPhone());
    }

    /** The row is created by the migration V7, so it is always there. */
    private BusinessSettings load() {
        return repository.findById(BusinessSettings.SINGLE_ROW_ID)
                .orElseThrow(() -> new IllegalStateException("business_settings row is missing"));
    }
}
