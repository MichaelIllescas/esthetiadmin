package com.esthetiadmin.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Own page format, so the API does not depend on how Spring serializes its Page. */
@Schema(description = "Página de resultados")
public record PageResponse<T>(
        @Schema(description = "Elementos de la página") List<T> content,
        @Schema(description = "Número de página, desde 0", example = "0") int page,
        @Schema(description = "Tamaño de página pedido", example = "20") int size,
        @Schema(description = "Total de elementos en todas las páginas", example = "57") long totalElements,
        @Schema(description = "Total de páginas", example = "3") int totalPages) {

    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
