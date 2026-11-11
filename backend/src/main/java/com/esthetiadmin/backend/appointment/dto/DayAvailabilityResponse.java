package com.esthetiadmin.backend.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Horarios libres de un día para un servicio")
public record DayAvailabilityResponse(
        @Schema(description = "Día", example = "2026-10-08") LocalDate date,
        @Schema(description = "Duración del turno, en minutos", example = "30") int durationMinutes,
        @Schema(description = "Horas de inicio libres (HH:mm)", example = "[\"09:00\", \"09:30\"]") List<String> times) {
}
