package com.esthetiadmin.backend.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo estado de activación")
public record ActiveRequest(
        @Schema(description = "true para activar, false para desactivar", example = "true")
        @NotNull(message = "Indicá si debe quedar activo o no") Boolean active) {
}
