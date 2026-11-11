package com.esthetiadmin.backend.expense.controller;

import com.esthetiadmin.backend.common.exception.ApiError;
import com.esthetiadmin.backend.expense.dto.ExpenseListResponse;
import com.esthetiadmin.backend.expense.dto.ExpenseRequest;
import com.esthetiadmin.backend.expense.dto.ExpenseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;

@Tag(name = "Gastos", description = "Registro y consulta de gastos")
public interface ExpenseControllerDocs {

    @Operation(summary = "Buscar gastos", description = "Todos los filtros son opcionales.")
    @ApiResponse(responseCode = "200", description = "Listado de gastos")
    @ApiResponse(responseCode = "400", description = "La fecha desde es posterior a la fecha hasta",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El tipo de gasto no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ExpenseListResponse search(
            @Parameter(description = "Fecha desde, incluida (yyyy-MM-dd)") LocalDate from,
            @Parameter(description = "Fecha hasta, incluida (yyyy-MM-dd)") LocalDate to,
            @Parameter(description = "Filtrar por tipo de gasto") Long expenseTypeId);

    @Operation(summary = "Registrar gasto")
    @ApiResponse(responseCode = "201", description = "Gasto registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o fecha futura",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El tipo de gasto no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ExpenseResponse create(ExpenseRequest request);

    @Operation(summary = "Modificar gasto")
    @ApiResponse(responseCode = "200", description = "Gasto modificado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o fecha futura",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El gasto o el tipo de gasto no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    ExpenseResponse update(@Parameter(description = "Id del gasto") Long id, ExpenseRequest request);

    @Operation(summary = "Eliminar gasto")
    @ApiResponse(responseCode = "204", description = "Gasto eliminado")
    @ApiResponse(responseCode = "404", description = "El gasto no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    void delete(@Parameter(description = "Id del gasto") Long id);
}
