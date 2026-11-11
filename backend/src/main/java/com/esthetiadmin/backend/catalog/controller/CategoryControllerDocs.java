package com.esthetiadmin.backend.catalog.controller;

import com.esthetiadmin.backend.catalog.dto.ActiveRequest;
import com.esthetiadmin.backend.catalog.dto.CategoryRequest;
import com.esthetiadmin.backend.catalog.dto.CategoryResponse;
import com.esthetiadmin.backend.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Categorías", description = "Categorías que agrupan los servicios ofrecidos")
public interface CategoryControllerDocs {

    @Operation(summary = "Listar categorías", description = "Devuelve todas las categorías.")
    @ApiResponse(responseCode = "200", description = "Listado de categorías")
    List<CategoryResponse> findAll();

    @Operation(summary = "Crear categoría")
    @ApiResponse(responseCode = "201", description = "Categoría creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o duración distinta de 30 o 60 minutos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CategoryResponse create(CategoryRequest request);

    @Operation(summary = "Modificar categoría", description = "Cambia el nombre y la duración de los turnos.")
    @ApiResponse(responseCode = "200", description = "Categoría modificada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o duración distinta de 30 o 60 minutos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "La categoría no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe una categoría con ese nombre",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CategoryResponse update(@Parameter(description = "Id de la categoría") Long id, CategoryRequest request);

    @Operation(summary = "Activar o desactivar categoría",
            description = "Una categoría inactiva deja sus servicios sin poder ofrecerse.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "La categoría no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    CategoryResponse setActive(@Parameter(description = "Id de la categoría") Long id, ActiveRequest request);

    @Operation(summary = "Eliminar categoría", description = "Solo se puede eliminar si no tiene servicios.")
    @ApiResponse(responseCode = "204", description = "Categoría eliminada")
    @ApiResponse(responseCode = "404", description = "La categoría no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "La categoría tiene servicios",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    void delete(@Parameter(description = "Id de la categoría") Long id);
}
