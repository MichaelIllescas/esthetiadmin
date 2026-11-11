package com.esthetiadmin.backend.catalog.dto;

import com.esthetiadmin.backend.catalog.entity.Offering;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Servicio ofrecido")
public record OfferingResponse(
        @Schema(description = "Id del servicio", example = "10") Long id,
        @Schema(description = "Id de la categoría", example = "1") Long categoryId,
        @Schema(description = "Nombre del servicio", example = "Limpieza facial profunda") String name,
        @Schema(description = "Descripción del servicio", example = "Incluye exfoliación y mascarilla") String description,
        @Schema(description = "Precio de lista", example = "15000.00") BigDecimal price,
        @Schema(description = "Si el servicio está activo", example = "true") boolean active,
        @Schema(description = "Si se ofrece en la página pública de turnos", example = "true") boolean bookable
) {

    public static OfferingResponse from(Offering offering) {
        return new OfferingResponse(offering.getId(), offering.getCategoryId(), offering.getName(),
                offering.getDescription(), offering.getPrice(), offering.isActive(), offering.isBookable());
    }
}
