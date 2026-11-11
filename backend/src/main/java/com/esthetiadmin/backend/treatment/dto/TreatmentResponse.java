package com.esthetiadmin.backend.treatment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Tratamiento registrado")
public record TreatmentResponse(
        @Schema(description = "Id del tratamiento", example = "100") Long id,
        @Schema(description = "Id del cliente", example = "5") Long clientId,
        @Schema(description = "Nombre completo del cliente", example = "Ana Gómez") String clientName,
        @Schema(description = "Id del servicio", example = "10") Long serviceId,
        @Schema(description = "Nombre del servicio", example = "Limpieza facial profunda") String serviceName,
        @Schema(description = "Id de la categoría del servicio", example = "1") Long categoryId,
        @Schema(description = "Nombre de la categoría", example = "Faciales") String categoryName,
        @Schema(description = "Fecha en que se realizó", example = "2026-09-29") LocalDate performedOn,
        @Schema(description = "Importe cobrado", example = "15000.00") BigDecimal amount,
        @Schema(description = "Observaciones", example = "Sin novedades") String notes
) {
}
