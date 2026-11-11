package com.esthetiadmin.backend.catalog.controller;

import com.esthetiadmin.backend.catalog.dto.ActiveRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingRequest;
import com.esthetiadmin.backend.catalog.dto.OfferingResponse;
import com.esthetiadmin.backend.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Servicios", description = "Servicios que se ofrecen, organizados por categoría")
public interface OfferingControllerDocs {

    @Operation(summary = "Crear servicio")
    @ApiResponse(responseCode = "201", description = "Servicio creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "La categoría no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un servicio con ese nombre",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    OfferingResponse create(OfferingRequest request);

    @Operation(summary = "Modificar servicio")
    @ApiResponse(responseCode = "200", description = "Servicio modificado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El servicio o la categoría no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Ya existe un servicio con ese nombre",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    OfferingResponse update(@Parameter(description = "Id del servicio") Long id, OfferingRequest request);

    @Operation(summary = "Activar o desactivar servicio",
            description = "Un servicio inactivo deja de ofrecerse, pero conserva sus tratamientos.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El servicio no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    OfferingResponse setActive(@Parameter(description = "Id del servicio") Long id, ActiveRequest request);

    @Operation(summary = "Eliminar servicio", description = "Solo se puede eliminar si no tiene tratamientos registrados.")
    @ApiResponse(responseCode = "204", description = "Servicio eliminado")
    @ApiResponse(responseCode = "404", description = "El servicio no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El servicio tiene tratamientos registrados",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    void delete(@Parameter(description = "Id del servicio") Long id);
}
