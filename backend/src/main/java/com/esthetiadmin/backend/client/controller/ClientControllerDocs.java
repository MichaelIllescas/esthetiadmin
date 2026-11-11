package com.esthetiadmin.backend.client.controller;

import com.esthetiadmin.backend.client.dto.BirthdayResponse;
import com.esthetiadmin.backend.client.dto.ClientRequest;
import com.esthetiadmin.backend.client.dto.ClientResponse;
import com.esthetiadmin.backend.client.dto.ExistsResponse;
import com.esthetiadmin.backend.common.dto.PageResponse;
import com.esthetiadmin.backend.common.exception.ApiError;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Clientes", description = "Alta, búsqueda y mantenimiento de clientes")
public interface ClientControllerDocs {

    @Operation(summary = "Buscar clientes", description = "Listado paginado, con búsqueda opcional por texto.")
    @ApiResponse(responseCode = "200", description = "Página de clientes")
    @ApiResponse(responseCode = "400", description = "Parámetros de paginación inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PageResponse<ClientResponse> search(
            @Parameter(description = "Texto a buscar en los datos del cliente") String search,
            @Parameter(description = "Número de página, desde 0") @Min(value = 0, message = "La página no puede ser negativa") int page,
            @Parameter(description = "Cantidad de clientes por página, de 1 a 100") @Min(value = 1, message = "El tamaño de página debe ser al menos 1") @Max(value = 100, message = "El tamaño de página no puede superar 100") int size);

    @Operation(summary = "Verificar si existe un cliente con ese nombre",
            description = "Sirve para avisar de un posible duplicado antes de guardar.")
    @ApiResponse(responseCode = "200", description = "Resultado de la verificación")
    ExistsResponse exists(
            @Parameter(description = "Nombre") String firstName,
            @Parameter(description = "Apellido") String lastName,
            @Parameter(description = "Id de un cliente a ignorar, útil al editar el propio cliente") Long excludeId);

    @Operation(summary = "Próximos cumpleaños", description = "Clientes que cumplen años dentro de la cantidad de días indicada.")
    @ApiResponse(responseCode = "200", description = "Listado de cumpleaños")
    @ApiResponse(responseCode = "400", description = "Cantidad de días inválida",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<BirthdayResponse> birthdays(@Parameter(description = "Días hacia adelante, de 1 a 366") @Min(value = 1, message = "Los días deben ser al menos 1") @Max(value = 366, message = "Los días no pueden superar 366") int days);

    @Operation(summary = "Obtener cliente")
    @ApiResponse(responseCode = "200", description = "Datos del cliente")
    @ApiResponse(responseCode = "404", description = "El cliente no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ClientResponse get(@Parameter(description = "Id del cliente") Long id);

    @Operation(summary = "Crear cliente")
    @ApiResponse(responseCode = "201", description = "Cliente creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ClientResponse create(@Valid ClientRequest request);

    @Operation(summary = "Modificar cliente")
    @ApiResponse(responseCode = "200", description = "Cliente modificado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El cliente no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ClientResponse update(@Parameter(description = "Id del cliente") Long id, @Valid ClientRequest request);

    @Operation(summary = "Eliminar cliente", description = "Solo se puede eliminar si no tiene tratamientos registrados.")
    @ApiResponse(responseCode = "204", description = "Cliente eliminado")
    @ApiResponse(responseCode = "404", description = "El cliente no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El cliente tiene tratamientos registrados",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    void delete(@Parameter(description = "Id del cliente") Long id);
}
