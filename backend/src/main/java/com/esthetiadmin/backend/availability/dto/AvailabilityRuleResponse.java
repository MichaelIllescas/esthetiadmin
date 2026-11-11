package com.esthetiadmin.backend.availability.dto;

import com.esthetiadmin.backend.availability.entity.AvailabilityRule;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;

@Schema(description = "Rango de atención de un día de la semana")
public record AvailabilityRuleResponse(
        @Schema(description = "Día de la semana: 1 = lunes ... 7 = domingo", example = "1") int dayOfWeek,
        @Schema(description = "Hora de inicio", example = "09:00", type = "string")
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @Schema(description = "Hora de fin", example = "13:00", type = "string")
        @JsonFormat(pattern = "HH:mm") LocalTime endTime) {

    public static AvailabilityRuleResponse from(AvailabilityRule rule) {
        return new AvailabilityRuleResponse(rule.getDayOfWeek(), rule.getStartTime(), rule.getEndTime());
    }
}
