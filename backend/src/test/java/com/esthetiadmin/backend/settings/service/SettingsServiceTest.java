package com.esthetiadmin.backend.settings.service;

import com.esthetiadmin.backend.common.exception.BadRequestException;
import com.esthetiadmin.backend.settings.dto.SettingsRequest;
import com.esthetiadmin.backend.settings.dto.SettingsResponse;
import com.esthetiadmin.backend.settings.entity.BusinessSettings;
import com.esthetiadmin.backend.settings.repository.BusinessSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SettingsServiceTest {

    private BusinessSettings settings;
    private SettingsService service;

    @BeforeEach
    void setUp() {
        settings = BeanUtils.instantiateClass(BusinessSettings.class);
        BusinessSettingsRepository repository = mock(BusinessSettingsRepository.class);
        when(repository.findById(BusinessSettings.SINGLE_ROW_ID)).thenReturn(Optional.of(settings));
        service = new SettingsService(repository);
    }

    @Test
    void updateNormalizesTheEmailAndThePhone() {
        SettingsResponse result = service.update(new SettingsRequest(" EsthetiAdmin@Mail.com ", "341 555-9876"));

        assertThat(result.notificationEmail()).isEqualTo("esthetiadmin@mail.com");
        assertThat(result.contactPhone()).isEqualTo("3415559876");
        assertThat(settings.getContactPhone()).isEqualTo("3415559876");
    }

    @Test
    void blankValuesClearTheSettings() {
        settings.update("esthetiadmin@mail.com", "3415559876");

        SettingsResponse result = service.update(new SettingsRequest("  ", null));

        assertThat(result.notificationEmail()).isNull();
        assertThat(result.contactPhone()).isNull();
    }

    @Test
    void rejectsAPhoneWithTooFewDigits() {
        assertThatThrownBy(() -> service.update(new SettingsRequest(null, "555")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void thePublicContactNeverIncludesTheNotificationEmail() {
        settings.update("esthetiadmin@mail.com", "3415559876");

        assertThat(service.publicContact().phone()).isEqualTo("3415559876");
    }
}
