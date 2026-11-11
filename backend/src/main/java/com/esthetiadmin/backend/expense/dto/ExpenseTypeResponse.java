package com.esthetiadmin.backend.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de gasto")
public record ExpenseTypeResponse(
        @Schema(description = "Id del tipo de gasto", example = "2") Long id,
        @Schema(description = "Nombre del tipo de gasto", example = "Insumos") String name) {
}
