package com.esthetiadmin.backend.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Gasto registrado")
public record ExpenseResponse(
        @Schema(description = "Id del gasto", example = "30") Long id,
        @Schema(description = "Fecha del gasto", example = "2026-09-29") LocalDate spentOn,
        @Schema(description = "Concepto", example = "Compra de insumos") String description,
        @Schema(description = "Importe", example = "8500.50") BigDecimal amount,
        @Schema(description = "Id del tipo de gasto", example = "2") Long expenseTypeId,
        @Schema(description = "Nombre del tipo de gasto", example = "Insumos") String expenseTypeName
) {
}
