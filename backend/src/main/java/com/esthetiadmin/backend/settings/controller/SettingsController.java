package com.esthetiadmin.backend.settings.controller;

import com.esthetiadmin.backend.settings.dto.SettingsRequest;
import com.esthetiadmin.backend.settings.dto.SettingsResponse;
import com.esthetiadmin.backend.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController implements SettingsControllerDocs {

    private final SettingsService settingsService;

    @Override
    @GetMapping
    public SettingsResponse get() {
        return settingsService.get();
    }

    @Override
    @PutMapping
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        return settingsService.update(request);
    }
}
