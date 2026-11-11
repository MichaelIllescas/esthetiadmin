package com.esthetiadmin.backend.availability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** An active appointment that falls inside a new block, so the owner can decide what to do with it. */
@Schema(description = "Turno activo que queda dentro de un bloqueo")
public record AffectedAppointmentResponse(
        @Schema(description = "Id del turno", example = "52") Long id,
        @Schema(description = "Inicio del turno", example = "2026-10-12T10:30:00") LocalDateTime startsAt,
        @Schema(description = "Nombre", example = "Carina") String firstName,
        @Schema(description = "Apellido", example = "Diaz") String lastName) {
}
