package com.esthetiadmin.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Income (treatments charged), expenses and the difference of a period. */
@Schema(description = "Ingresos (tratamientos cobrados), gastos y diferencia de un período")
public record SummaryResponse(
        @Schema(description = "Inicio del período", example = "2026-09-01") LocalDate from,
        @Schema(description = "Fin del período", example = "2026-09-30") LocalDate to,
        @Schema(description = "Total de ingresos", example = "450000.00") BigDecimal income,
        @Schema(description = "Total de gastos", example = "120000.00") BigDecimal expenses,
        @Schema(description = "Ingresos menos gastos", example = "330000.00") BigDecimal balance,
        @Schema(description = "Cantidad de tratamientos", example = "30") long treatmentsCount,
        @Schema(description = "Cantidad de gastos", example = "8") long expensesCount
) {
}
