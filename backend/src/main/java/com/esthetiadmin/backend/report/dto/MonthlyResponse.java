package com.esthetiadmin.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** One month ("2026-09") of the monthly evolution. Months with no movement are included with zeros. */
@Schema(description = "Un mes de la evolución mensual. Los meses sin movimiento se incluyen con ceros")
public record MonthlyResponse(
        @Schema(description = "Mes en formato año-mes", example = "2026-09") String month,
        @Schema(description = "Ingresos del mes", example = "150000.00") BigDecimal income,
        @Schema(description = "Gastos del mes", example = "40000.00") BigDecimal expenses,
        @Schema(description = "Ingresos menos gastos", example = "110000.00") BigDecimal balance) {
}
