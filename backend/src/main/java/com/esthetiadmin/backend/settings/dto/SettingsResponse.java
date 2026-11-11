package com.esthetiadmin.backend.settings.dto;

import com.esthetiadmin.backend.settings.entity.BusinessSettings;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de contacto del negocio")
public record SettingsResponse(
        @Schema(description = "Email de avisos; null si no está cargado", example = "esthetiadmin@mail.com")
        String notificationEmail,
        @Schema(description = "Celular de contacto, solo dígitos; null si no está cargado", example = "3415559876")
        String contactPhone
) {

    public static SettingsResponse from(BusinessSettings settings) {
        return new SettingsResponse(settings.getNotificationEmail(), settings.getContactPhone());
    }
}
