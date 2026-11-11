package com.esthetiadmin.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciales para iniciar sesión")
public record LoginRequest(
        @Schema(description = "Nombre de usuario", example = "admin")
        @NotBlank(message = "El usuario es obligatorio") String username,
        @Schema(description = "Contraseña del usuario", example = "mi-contraseña-123", format = "password")
        @NotBlank(message = "La contraseña es obligatoria") String password
) {

    /** Keeps the password out of logs. */
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
