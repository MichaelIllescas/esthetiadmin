package com.esthetiadmin.backend.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Datos para registrar o modificar un gasto")
public record ExpenseRequest(
        @Schema(description = "Fecha del gasto, no futura", example = "2026-09-29")
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura") LocalDate spentOn,
        @Schema(description = "Concepto del gasto", example = "Compra de insumos")
        @NotBlank(message = "El concepto es obligatorio")
        @Size(max = 255, message = "El concepto no puede superar los 255 caracteres") String description,
        @Schema(description = "Importe, mayor que cero, hasta 2 decimales", example = "8500.50")
        @NotNull(message = "El importe es obligatorio")
        @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
        @Digits(integer = 8, fraction = 2, message = "El importe no es válido") BigDecimal amount,
        @Schema(description = "Id del tipo de gasto", example = "2")
        @NotNull(message = "El tipo de gasto es obligatorio") Long expenseTypeId
) {
}
