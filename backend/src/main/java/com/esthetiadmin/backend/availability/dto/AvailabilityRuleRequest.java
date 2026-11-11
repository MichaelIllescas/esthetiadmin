package com.esthetiadmin.backend.availability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

/** One range of the weekly schedule. Validated by the service, together with the rest of the week. */
@Schema(description = "Rango de atención de un día de la semana")
public record AvailabilityRuleRequest(
        @Schema(description = "Día de la semana: 1 = lunes ... 7 = domingo", example = "1") Integer dayOfWeek,
        @Schema(description = "Hora de inicio, en múltiplos de 30 minutos", example = "09:00", type = "string")
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @Schema(description = "Hora de fin, posterior al inicio y en múltiplos de 30 minutos", example = "13:00",
                type = "string")
        @JsonFormat(pattern = "HH:mm") LocalTime endTime) {
}
