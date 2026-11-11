package com.esthetiadmin.backend.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/** Income of the period grouped by category and, inside each one, by service. */
@Schema(description = "Ingresos del período agrupados por categoría y, dentro de cada una, por servicio")
public record IncomeBreakdownResponse(
        @Schema(description = "Ingreso total del período", example = "450000.00") BigDecimal totalIncome,
        @Schema(description = "Ingresos por categoría") List<CategoryIncome> categories) {

    /** {@code percentage} is the share of the total income of the period, with two decimals. */
    @Schema(description = "Ingresos de una categoría")
    public record CategoryIncome(
            @Schema(description = "Id de la categoría", example = "1") Long categoryId,
            @Schema(description = "Nombre de la categoría", example = "Faciales") String categoryName,
            @Schema(description = "Ingreso de la categoría", example = "180000.00") BigDecimal income,
            @Schema(description = "Porcentaje sobre el ingreso total, con 2 decimales", example = "40.00") BigDecimal percentage,
            @Schema(description = "Cantidad de tratamientos", example = "12") long treatments,
            @Schema(description = "Detalle por servicio") List<ServiceIncome> services
    ) {
    }

    @Schema(description = "Ingresos de un servicio")
    public record ServiceIncome(
            @Schema(description = "Id del servicio", example = "10") Long serviceId,
            @Schema(description = "Nombre del servicio", example = "Limpieza facial profunda") String serviceName,
            @Schema(description = "Ingreso del servicio", example = "90000.00") BigDecimal income,
            @Schema(description = "Porcentaje sobre el ingreso total, con 2 decimales", example = "20.00") BigDecimal percentage,
            @Schema(description = "Cantidad de tratamientos", example = "6") long treatments
    ) {
    }
}
