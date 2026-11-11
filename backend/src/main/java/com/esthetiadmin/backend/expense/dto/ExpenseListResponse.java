package com.esthetiadmin.backend.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Listado de gastos con su total")
public record ExpenseListResponse(
        @Schema(description = "Gastos que cumplen los filtros") List<ExpenseResponse> content,
        @Schema(description = "Suma de los importes de los gastos listados", example = "25400.75") BigDecimal totalAmount) {
}
