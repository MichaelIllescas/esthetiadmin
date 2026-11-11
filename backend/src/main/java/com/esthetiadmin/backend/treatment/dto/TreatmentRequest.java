package com.esthetiadmin.backend.treatment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Datos para registrar un tratamiento")
public record TreatmentRequest(
        @Schema(description = "Id del cliente atendido", example = "5")
        @NotNull(message = "El cliente es obligatorio") Long clientId,
        @Schema(description = "Id del servicio realizado", example = "10")
        @NotNull(message = "El servicio es obligatorio") Long serviceId,
        @Schema(description = "Fecha en que se realizó, no futura", example = "2026-09-29")
        @NotNull(message = "La fecha es obligatoria")
        @PastOrPresent(message = "La fecha no puede ser futura") LocalDate performedOn,
        @Schema(description = "Importe cobrado, no negativo, hasta 2 decimales", example = "15000.00")
        @NotNull(message = "El importe es obligatorio")
        @DecimalMin(value = "0.00", message = "El importe no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El importe no es válido") BigDecimal amount,
        @Schema(description = "Observaciones, opcional", example = "Sin novedades")
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres") String notes
) {
}
