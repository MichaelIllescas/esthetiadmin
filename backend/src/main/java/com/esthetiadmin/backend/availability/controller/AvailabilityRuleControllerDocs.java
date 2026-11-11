package com.esthetiadmin.backend.availability.controller;

import com.esthetiadmin.backend.availability.dto.AvailabilityRuleRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityRuleResponse;
import com.esthetiadmin.backend.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Horarios de atención", description = "Rangos de atención por día de la semana para los turnos")
public interface AvailabilityRuleControllerDocs {

    @Operation(summary = "Listar horarios de atención", description = "Ordenados por día y hora de inicio.")
    @ApiResponse(responseCode = "200", description = "Horarios de la semana")
    List<AvailabilityRuleResponse> findAll();

    @Operation(summary = "Reemplazar los horarios de atención",
            description = "Recibe la semana completa. Un día sin rangos queda cerrado; una lista vacía cierra todos. "
                    + "No modifica turnos ya tomados.")
    @ApiResponse(responseCode = "200", description = "Horarios guardados")
    @ApiResponse(responseCode = "400",
            description = "Día fuera de 1 a 7, horas fuera de la grilla de 30 minutos, inicio no anterior al fin "
                    + "o rangos superpuestos en un mismo día",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<AvailabilityRuleResponse> replaceAll(List<AvailabilityRuleRequest> request);
}
