package com.esthetiadmin.backend.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contacto público del centro: solo el celular, nunca el email de avisos")
public record PublicContactResponse(
        @Schema(description = "Celular para cancelar o reagendar, solo dígitos; null si no está cargado",
                example = "3415559876") String phone
) {
}
