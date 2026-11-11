package com.esthetiadmin.backend.catalog.dto;

import com.esthetiadmin.backend.catalog.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Categoría con los servicios que contiene")
public record CategoryResponse(
        @Schema(description = "Id de la categoría", example = "1") Long id,
        @Schema(description = "Nombre de la categoría", example = "Faciales") String name,
        @Schema(description = "Si la categoría está activa", example = "true") boolean active,
        @Schema(description = "Duración de los turnos de sus servicios, en minutos", example = "60") int appointmentMinutes,
        @Schema(description = "Servicios de la categoría") List<OfferingResponse> services) {

    public static CategoryResponse from(Category category, List<OfferingResponse> services) {
        return new CategoryResponse(category.getId(), category.getName(), category.isActive(),
                category.getAppointmentMinutes(), services);
    }
}
