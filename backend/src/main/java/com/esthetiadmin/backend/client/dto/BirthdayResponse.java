package com.esthetiadmin.backend.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Cliente con cumpleaños próximo")
public record BirthdayResponse(
        @Schema(description = "Id del cliente", example = "5") Long id,
        @Schema(description = "Nombre", example = "Ana") String firstName,
        @Schema(description = "Apellido", example = "Gómez") String lastName,
        @Schema(description = "Fecha de nacimiento", example = "1990-10-05") LocalDate birthDate,
        @Schema(description = "Fecha del próximo cumpleaños", example = "2026-10-05") LocalDate nextBirthday,
        @Schema(description = "Edad que cumple", example = "36") int turningAge,
        @Schema(description = "Días que faltan", example = "6") long daysUntil
) {
}
