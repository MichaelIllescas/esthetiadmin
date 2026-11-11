package com.esthetiadmin.backend.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Only the summary the person needs: no internal id nor anybody else's data. */
@Schema(description = "Resumen del turno reservado")
public record PublicBookingResponse(
        @Schema(description = "Servicio", example = "Bozo") String serviceName,
        @Schema(description = "Inicio", example = "2026-10-08T10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startsAt,
        @Schema(description = "Fin", example = "2026-10-08T11:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endsAt,
        @Schema(description = "Nombre", example = "Carina") String firstName,
        @Schema(description = "Si se pidió el email de confirmación (no garantiza que llegue)", example = "true")
        boolean emailSent) {
}
