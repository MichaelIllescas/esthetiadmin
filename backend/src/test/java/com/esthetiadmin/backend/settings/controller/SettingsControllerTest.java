package com.esthetiadmin.backend.settings.controller;

import com.esthetiadmin.backend.auth.security.ApiSecurityErrorHandler;
import com.esthetiadmin.backend.auth.security.JwtConfig;
import com.esthetiadmin.backend.auth.security.SecurityConfig;
import com.esthetiadmin.backend.common.config.CorsConfig;
import com.esthetiadmin.backend.common.exception.GlobalExceptionHandler;
import com.esthetiadmin.backend.settings.dto.PublicContactResponse;
import com.esthetiadmin.backend.settings.dto.SettingsResponse;
import com.esthetiadmin.backend.settings.service.SettingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SettingsController.class, PublicContactController.class})
@Import({SecurityConfig.class, ApiSecurityErrorHandler.class, JwtConfig.class, CorsConfig.class,
        GlobalExceptionHandler.class})
@TestPropertySource(properties = {
        "app.security.jwt.secret=test-secret-with-more-than-thirty-two-characters",
        "app.security.jwt.expiration-minutes=60",
        "app.cors.allowed-origins=http://localhost:4200"
})
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettingsService settingsService;

    @Test
    void settingsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsTheSettings() throws Exception {
        when(settingsService.get()).thenReturn(new SettingsResponse("esthetiadmin@mail.com", "3415559876"));

        mockMvc.perform(get("/api/settings").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationEmail").value("esthetiadmin@mail.com"))
                .andExpect(jsonPath("$.contactPhone").value("3415559876"));
    }

    @Test
    void savesTheSettings() throws Exception {
        when(settingsService.update(any())).thenReturn(new SettingsResponse("esthetiadmin@mail.com", null));

        mockMvc.perform(put("/api/settings").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationEmail\":\"esthetiadmin@mail.com\",\"contactPhone\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationEmail").value("esthetiadmin@mail.com"));
    }

    @Test
    void rejectsAnInvalidEmail() throws Exception {
        mockMvc.perform(put("/api/settings").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationEmail\":\"no-es-un-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El email de avisos no es válido"));
    }

    @Test
    void thePublicContactNeedsNoLoginAndOnlyHasThePhone() throws Exception {
        when(settingsService.publicContact()).thenReturn(new PublicContactResponse("3415559876"));

        mockMvc.perform(get("/api/public/contact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("3415559876"))
                .andExpect(jsonPath("$.notificationEmail").doesNotExist());
    }
}
