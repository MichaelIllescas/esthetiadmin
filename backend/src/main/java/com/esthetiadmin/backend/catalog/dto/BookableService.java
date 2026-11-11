package com.esthetiadmin.backend.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** A service as the public booking page shows it: never with its price. */
@Schema(description = "Servicio que se puede reservar online")
public record BookableService(
        @Schema(description = "Id del servicio", example = "10") Long id,
        @Schema(description = "Nombre", example = "Bozo") String name,
        @Schema(description = "Descripción", example = "Zona del rostro") String description) {
}
