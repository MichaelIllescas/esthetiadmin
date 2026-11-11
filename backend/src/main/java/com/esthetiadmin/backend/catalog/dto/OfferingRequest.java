package com.esthetiadmin.backend.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Datos para crear o modificar un servicio")
public record OfferingRequest(
        @Schema(description = "Id de la categoría a la que pertenece", example = "1")
        @NotNull(message = "La categoría es obligatoria") Long categoryId,
        @Schema(description = "Nombre del servicio, único", example = "Limpieza facial profunda")
        @NotBlank(message = "El nombre del servicio es obligatorio")
        @Size(max = 150, message = "El nombre del servicio no puede superar los 150 caracteres") String name,
        @Schema(description = "Descripción opcional del servicio", example = "Incluye exfoliación y mascarilla")
        @Size(max = 2000, message = "La descripción no puede superar los 2000 caracteres") String description,
        @Schema(description = "Precio de lista, no negativo, hasta 2 decimales", example = "15000.00")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El precio no es válido") BigDecimal price,
        @Schema(description = "Si se ofrece en la página pública de turnos. Si se omite, true al crear y sin cambios "
                + "al modificar", example = "true")
        Boolean bookable
) {
}
