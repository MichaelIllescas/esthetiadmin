package com.esthetiadmin.backend.appointment.controller;

import com.esthetiadmin.backend.appointment.dto.AvailableDaysResponse;
import com.esthetiadmin.backend.appointment.dto.DayAvailabilityResponse;
import com.esthetiadmin.backend.appointment.dto.PublicBookingRequest;
import com.esthetiadmin.backend.appointment.dto.PublicBookingResponse;
import com.esthetiadmin.backend.catalog.dto.BookableCategory;
import com.esthetiadmin.backend.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Turnos (público)", description = "Reserva de turnos sin login desde la página pública")
@SecurityRequirements
public interface PublicBookingControllerDocs {

    @Operation(summary = "Servicios reservables",
            description = "Categorías y servicios activos marcados como reservables online, sin precio.")
    @ApiResponse(responseCode = "200", description = "Servicios por categoría")
    List<BookableCategory> services();

    @Operation(summary = "Días con lugar en un mes",
            description = "Días del mes, desde hoy y hasta 60 días adelante, con al menos un horario libre.")
    @ApiResponse(responseCode = "200", description = "Días con lugar")
    @ApiResponse(responseCode = "404", description = "El servicio no existe o no se puede reservar online",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AvailableDaysResponse availableDays(@Parameter(description = "Id del servicio") Long serviceId,
                                        @Parameter(description = "Mes (yyyy-MM)", example = "2026-10") YearMonth month);

    @Operation(summary = "Horarios libres de un día")
    @ApiResponse(responseCode = "200", description = "Horas de inicio libres")
    @ApiResponse(responseCode = "400", description = "Día fuera de hoy a 60 días adelante",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El servicio no existe o no se puede reservar online",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    DayAvailabilityResponse availability(@Parameter(description = "Id del servicio") Long serviceId,
                                         @Parameter(description = "Día (yyyy-MM-dd)") LocalDate date);

    @Operation(summary = "Reservar turno", description = "Queda confirmado. Devuelve solo el resumen.")
    @ApiResponse(responseCode = "201", description = "Turno reservado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o día fuera de la ventana de 60 días",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El servicio no existe o no se puede reservar online",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El horario ya no está libre o el teléfono ya tiene 2 turnos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "429", description = "Más de 5 pedidos por hora desde la misma IP",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    PublicBookingResponse book(PublicBookingRequest request, @Parameter(hidden = true) HttpServletRequest httpRequest);
}
