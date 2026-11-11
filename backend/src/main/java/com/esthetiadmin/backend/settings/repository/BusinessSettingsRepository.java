package com.esthetiadmin.backend.settings.repository;

import com.esthetiadmin.backend.settings.entity.BusinessSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {
}
