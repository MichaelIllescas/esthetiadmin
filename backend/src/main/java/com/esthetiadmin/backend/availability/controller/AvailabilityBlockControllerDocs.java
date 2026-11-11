package com.esthetiadmin.backend.availability.controller;

import com.esthetiadmin.backend.availability.dto.AvailabilityBlockRequest;
import com.esthetiadmin.backend.availability.dto.AvailabilityBlockResponse;
import com.esthetiadmin.backend.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Bloqueos de agenda", description = "Fechas o rangos sacados de la agenda de turnos (feriados, vacaciones)")
public interface AvailabilityBlockControllerDocs {

    @Operation(summary = "Listar bloqueos",
            description = "Los que se cruzan con `from` (hoy si se omite) a `to` (sin límite si se omite), ambos "
                    + "incluidos: sin `from`, los que ya terminaron no aparecen.")
    @ApiResponse(responseCode = "200", description = "Bloqueos ordenados por fecha y hora")
    @ApiResponse(responseCode = "400", description = "`from` posterior a `to` o fecha mal formada",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<AvailabilityBlockResponse> search(
            @Parameter(description = "Desde (yyyy-MM-dd)") LocalDate from,
            @Parameter(description = "Hasta (yyyy-MM-dd)") LocalDate to);

    @Operation(summary = "Crear bloqueo",
            description = "Bloquea de `startDate` a `endDate`. Sin horas bloquea los días completos; las horas solo "
                    + "valen para un bloqueo de un día. No cancela turnos: devuelve los que quedan dentro.")
    @ApiResponse(responseCode = "201", description = "Bloqueo creado")
    @ApiResponse(responseCode = "400",
            description = "Fecha pasada, fin anterior al inicio, más de 366 días, horas en un bloqueo de varios días, "
                    + "una sola hora indicada, horas fuera de la grilla de 30 minutos o inicio no anterior al fin",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AvailabilityBlockResponse create(AvailabilityBlockRequest request);

    @Operation(summary = "Eliminar bloqueo")
    @ApiResponse(responseCode = "204", description = "Bloqueo eliminado")
    @ApiResponse(responseCode = "404", description = "El bloqueo no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    void delete(@Parameter(description = "Id del bloqueo") Long id);
}
