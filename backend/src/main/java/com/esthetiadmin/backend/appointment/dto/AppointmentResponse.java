package com.esthetiadmin.backend.appointment.dto;

import com.esthetiadmin.backend.appointment.entity.AppointmentSource;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Turno")
public record AppointmentResponse(
        @Schema(description = "Id del turno", example = "52") Long id,
        @Schema(description = "Id del servicio", example = "10") Long serviceId,
        @Schema(description = "Nombre del servicio", example = "Bozo") String serviceName,
        @Schema(description = "Nombre de la categoría", example = "Depilación") String categoryName,
        @Schema(description = "Inicio", example = "2026-10-08T10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime startsAt,
        @Schema(description = "Fin", example = "2026-10-08T11:00:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime endsAt,
        @Schema(description = "Nombre", example = "Carina") String firstName,
        @Schema(description = "Apellido", example = "Diaz") String lastName,
        @Schema(description = "Teléfono, solo números", example = "3415551234") String phone,
        @Schema(description = "Id del cliente vinculado", example = "7", nullable = true) Long clientId,
        @Schema(description = "Estado", example = "CONFIRMED") AppointmentStatus status,
        @Schema(description = "Origen: la página pública o el panel", example = "PUBLIC") AppointmentSource source,
        @Schema(description = "Observaciones", example = "Primera vez") String notes,
        @Schema(description = "Email para avisos; null si no tiene", example = "carina@mail.com") String email) {
}
