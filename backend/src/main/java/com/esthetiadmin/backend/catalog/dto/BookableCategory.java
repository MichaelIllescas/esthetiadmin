package com.esthetiadmin.backend.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Categoría con sus servicios que se pueden reservar online")
public record BookableCategory(
        @Schema(description = "Id de la categoría", example = "1") Long categoryId,
        @Schema(description = "Nombre de la categoría", example = "Depilación") String categoryName,
        @Schema(description = "Duración de sus turnos, en minutos", example = "30") int durationMinutes,
        @Schema(description = "Servicios reservables") List<BookableService> services) {
}
