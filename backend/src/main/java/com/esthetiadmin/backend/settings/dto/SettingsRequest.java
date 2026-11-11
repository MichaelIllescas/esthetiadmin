package com.esthetiadmin.backend.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos de contacto del negocio; los dos son opcionales y null los borra")
public record SettingsRequest(
        @Schema(description = "Email donde la responsable recibe los avisos de reservas públicas",
                example = "esthetiadmin@mail.com")
        @Email(message = "El email de avisos no es válido")
        @Size(max = 254, message = "El email de avisos no puede superar los 254 caracteres") String notificationEmail,
        @Schema(description = "Celular para que las clientas cancelen o reagenden", example = "341 555-9876")
        @Size(max = 30, message = "El celular no puede superar los 30 caracteres") String contactPhone
) {
}
