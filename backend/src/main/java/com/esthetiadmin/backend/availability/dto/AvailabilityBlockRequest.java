package com.esthetiadmin.backend.availability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Bloqueo de la agenda: sin horas bloquea los días completos")
public record AvailabilityBlockRequest(
        @Schema(description = "Primer día bloqueado, de hoy en adelante", example = "2027-01-04")
        @NotNull(message = "La fecha de inicio del bloqueo es obligatoria") LocalDate startDate,
        @Schema(description = "Último día bloqueado; igual a startDate para un solo día", example = "2027-01-15")
        @NotNull(message = "La fecha de fin del bloqueo es obligatoria") LocalDate endDate,
        @Schema(description = "Hora de inicio, en múltiplos de 30 minutos, solo para un bloqueo de un día; vacía para días completos",
                example = "15:00", type = "string", nullable = true)
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @Schema(description = "Hora de fin, en múltiplos de 30 minutos, solo para un bloqueo de un día; vacía para días completos",
                example = "19:00", type = "string", nullable = true)
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @Schema(description = "Motivo opcional", example = "Feriado")
        @Size(max = 150, message = "El motivo no puede superar los 150 caracteres") String reason) {
}
