package com.esthetiadmin.backend.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de la verificación de duplicados")
public record ExistsResponse(
        @Schema(description = "true si ya existe un cliente con ese nombre y apellido", example = "false") boolean exists) {
}
