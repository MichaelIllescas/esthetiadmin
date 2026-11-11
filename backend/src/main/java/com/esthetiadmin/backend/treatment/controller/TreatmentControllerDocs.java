package com.esthetiadmin.backend.treatment.controller;

import com.esthetiadmin.backend.common.dto.PageResponse;
import com.esthetiadmin.backend.common.exception.ApiError;
import com.esthetiadmin.backend.treatment.dto.TreatmentRequest;
import com.esthetiadmin.backend.treatment.dto.TreatmentResponse;
import com.esthetiadmin.backend.treatment.dto.TreatmentUpdateRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Tag(name = "Tratamientos", description = "Registro de los tratamientos realizados a los clientes")
public interface TreatmentControllerDocs {

    @Operation(summary = "Buscar tratamientos",
            description = "Listado paginado, del más reciente al más antiguo. Todos los filtros son opcionales.")
    @ApiResponse(responseCode = "200", description = "Página de tratamientos")
    @ApiResponse(responseCode = "400", description = "Filtros o paginación inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<TreatmentResponse> search(
            @Parameter(description = "Filtrar por cliente") Long clientId,
            @Parameter(description = "Filtrar por servicio") Long serviceId,
            @Parameter(description = "Filtrar por categoría") Long categoryId,
            @Parameter(description = "Fecha desde, incluida (yyyy-MM-dd)") LocalDate from,
            @Parameter(description = "Fecha hasta, incluida (yyyy-MM-dd)") LocalDate to,
            @Parameter(description = "Número de página, desde 0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Cantidad por página, de 1 a 100") @Min(value = 1, message = "El tamaño de página debe ser al menos 1") @Max(value = 100, message = "El tamaño de página no puede superar 100") int size);

    @Operation(summary = "Registrar tratamiento")
    @ApiResponse(responseCode = "201", description = "Tratamiento registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El cliente o el servicio no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El servicio no está disponible (desactivado o de categoría inactiva)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    TreatmentResponse create(@Valid TreatmentRequest request);

    @Operation(summary = "Modificar tratamiento")
    @ApiResponse(responseCode = "200", description = "Tratamiento modificado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El tratamiento o el servicio no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El servicio no está disponible",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    TreatmentResponse update(@Parameter(description = "Id del tratamiento") Long id, @Valid TreatmentUpdateRequest request);

    @Operation(summary = "Eliminar tratamiento")
    @ApiResponse(responseCode = "204", description = "Tratamiento eliminado")
    @ApiResponse(responseCode = "404", description = "El tratamiento no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    void delete(@Parameter(description = "Id del tratamiento") Long id);
}
