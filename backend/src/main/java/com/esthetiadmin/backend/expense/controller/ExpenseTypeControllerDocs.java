package com.esthetiadmin.backend.expense.controller;

import com.esthetiadmin.backend.expense.dto.ExpenseTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Tipos de gasto", description = "Tipos disponibles para clasificar los gastos")
public interface ExpenseTypeControllerDocs {

    @Operation(summary = "Listar tipos de gasto")
    @ApiResponse(responseCode = "200", description = "Listado de tipos de gasto")
    List<ExpenseTypeResponse> list();
}
