package com.esthetiadmin.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Cliente dentro del ranking del período")
public record TopClientResponse(
        @Schema(description = "Id del cliente", example = "5") Long clientId,
        @Schema(description = "Nombre completo del cliente", example = "Ana Gómez") String clientName,
        @Schema(description = "Cantidad de tratamientos", example = "4") long treatments,
        @Schema(description = "Total pagado", example = "60000.00") BigDecimal totalAmount) {
}
