package com.esthetiadmin.backend.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o modificar una categoría")
public record CategoryRequest(
        @Schema(description = "Nombre de la categoría, único", example = "Faciales")
        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre de la categoría no puede superar los 100 caracteres") String name,
        @Schema(description = "Duración de los turnos de sus servicios, en minutos: 30 o 60. Si se omite, 60 al crear "
                + "y sin cambios al modificar", example = "60", allowableValues = {"30", "60"})
        Integer appointmentMinutes
) {
}
