package com.esthetiadmin.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Servicio dentro del ranking del período")
public record TopServiceResponse(
        @Schema(description = "Id del servicio", example = "10") Long serviceId,
        @Schema(description = "Nombre del servicio", example = "Limpieza facial profunda") String serviceName,
        @Schema(description = "Id de la categoría", example = "1") Long categoryId,
        @Schema(description = "Nombre de la categoría", example = "Faciales") String categoryName,
        @Schema(description = "Cantidad de tratamientos", example = "12") long treatments,
        @Schema(description = "Ingreso generado", example = "180000.00") BigDecimal income
) {
}
