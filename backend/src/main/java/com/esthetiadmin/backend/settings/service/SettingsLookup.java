package com.esthetiadmin.backend.settings.service;

import com.esthetiadmin.backend.settings.dto.BusinessContact;
import com.esthetiadmin.backend.settings.entity.BusinessSettings;
import com.esthetiadmin.backend.settings.repository.BusinessSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The read-only door of the settings module for the rest of the modules, which must not reach its repository. */
@Service
@RequiredArgsConstructor
public class SettingsLookup {

    private final BusinessSettingsRepository repository;

    @Transactional(readOnly = true)
    public BusinessContact contact() {
        return repository.findById(BusinessSettings.SINGLE_ROW_ID)
                .map(settings -> new BusinessContact(settings.getNotificationEmail(), settings.getContactPhone()))
                .orElse(new BusinessContact(null, null));
    }
}
