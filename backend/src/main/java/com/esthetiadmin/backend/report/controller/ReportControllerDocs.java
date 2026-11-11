package com.esthetiadmin.backend.report.controller;

import com.esthetiadmin.backend.common.exception.ApiError;
import com.esthetiadmin.backend.report.dto.IncomeBreakdownResponse;
import com.esthetiadmin.backend.report.dto.MonthlyResponse;
import com.esthetiadmin.backend.report.dto.SummaryResponse;
import com.esthetiadmin.backend.report.dto.TopClientResponse;
import com.esthetiadmin.backend.report.dto.TopServiceResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Reportes", description = "Reportes financieros de un período. Las fechas desde y hasta son obligatorias e incluidas.")
public interface ReportControllerDocs {

    @Operation(summary = "Resumen del período", description = "Totales de ingresos, gastos y resultado del período.")
    @ApiResponse(responseCode = "200", description = "Resumen del período")
    @ApiResponse(responseCode = "400", description = "Período faltante, invertido o demasiado largo",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    SummaryResponse summary(
            @Parameter(description = "Fecha desde (yyyy-MM-dd)") LocalDate from,
            @Parameter(description = "Fecha hasta (yyyy-MM-dd)") LocalDate to);

    @Operation(summary = "Evolución mensual", description = "Ingresos y gastos mes a mes dentro del período.")
    @ApiResponse(responseCode = "200", description = "Datos por mes")
    @ApiResponse(responseCode = "400", description = "Período faltante, invertido o demasiado largo",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<MonthlyResponse> monthly(
            @Parameter(description = "Fecha desde (yyyy-MM-dd)") LocalDate from,
            @Parameter(description = "Fecha hasta (yyyy-MM-dd)") LocalDate to);

    @Operation(summary = "Servicios más realizados", description = "Ranking de servicios del período.")
    @ApiResponse(responseCode = "200", description = "Ranking de servicios")
    @ApiResponse(responseCode = "400", description = "Período o límite inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<TopServiceResponse> topServices(
            @Parameter(description = "Fecha desde (yyyy-MM-dd)") LocalDate from,
            @Parameter(description = "Fecha hasta (yyyy-MM-dd)") LocalDate to,
            @Parameter(description = "Cantidad máxima de servicios, de 1 a 100") @Min(value = 1, message = "El límite debe ser al menos 1") @Max(value = 100, message = "El límite no puede superar 100") int limit);

    @Operation(summary = "Ingresos por servicio", description = "Detalle de ingresos agrupado por categoría y servicio.")
    @ApiResponse(responseCode = "200", description = "Desglose de ingresos")
    @ApiResponse(responseCode = "400", description = "Período faltante, invertido o demasiado largo",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    IncomeBreakdownResponse incomeByService(
            @Parameter(description = "Fecha desde (yyyy-MM-dd)") LocalDate from,
            @Parameter(description = "Fecha hasta (yyyy-MM-dd)") LocalDate to);

    @Operation(summary = "Mejores clientes", description = "Ranking de clientes del período, por monto o por cantidad de tratamientos.")
    @ApiResponse(responseCode = "200", description = "Ranking de clientes")
    @ApiResponse(responseCode = "400", description = "Período, límite u orden inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<TopClientResponse> topClients(
            @Parameter(description = "Fecha desde (yyyy-MM-dd)") LocalDate from,
            @Parameter(description = "Fecha hasta (yyyy-MM-dd)") LocalDate to,
            @Parameter(description = "Cantidad máxima de clientes, de 1 a 100") @Min(value = 1, message = "El límite debe ser al menos 1") @Max(value = 100, message = "El límite no puede superar 100") int limit,
            @Parameter(description = "Orden del ranking: amount (por defecto) o treatments",
                    schema = @Schema(allowableValues = {"amount", "treatments"})) String sortBy);
}
