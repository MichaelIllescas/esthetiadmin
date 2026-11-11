package com.esthetiadmin.backend.settings.controller;

import com.esthetiadmin.backend.settings.dto.PublicContactResponse;
import com.esthetiadmin.backend.settings.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/contact")
@RequiredArgsConstructor
public class PublicContactController implements PublicContactControllerDocs {

    private final SettingsService settingsService;

    @Override
    @GetMapping
    public PublicContactResponse get() {
        return settingsService.publicContact();
    }
}
