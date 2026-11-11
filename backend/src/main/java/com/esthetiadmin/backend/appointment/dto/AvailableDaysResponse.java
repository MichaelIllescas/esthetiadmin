package com.esthetiadmin.backend.appointment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Schema(description = "Días de un mes con al menos un horario libre para un servicio")
public record AvailableDaysResponse(
        @Schema(description = "Mes", example = "2026-10", type = "string")
        @JsonFormat(pattern = "yyyy-MM") YearMonth month,
        @Schema(description = "Días con lugar", example = "[\"2026-10-06\", \"2026-10-07\"]") List<LocalDate> days) {
}
