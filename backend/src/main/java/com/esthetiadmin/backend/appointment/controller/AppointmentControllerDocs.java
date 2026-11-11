package com.esthetiadmin.backend.appointment.controller;

import com.esthetiadmin.backend.appointment.dto.AppointmentClientRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentNoticeRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentRequest;
import com.esthetiadmin.backend.appointment.dto.AppointmentResponse;
import com.esthetiadmin.backend.appointment.dto.AppointmentStatusRequest;
import com.esthetiadmin.backend.appointment.dto.DayAvailabilityResponse;
import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import com.esthetiadmin.backend.common.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Turnos", description = "Agenda de turnos desde el panel")
public interface AppointmentControllerDocs {

    @Operation(summary = "Listar la agenda", description = "Turnos de `from` a `to` (ambos incluidos, hasta 62 días), "
            + "ordenados por hora.")
    @ApiResponse(responseCode = "200", description = "Turnos del rango")
    @ApiResponse(responseCode = "400", description = "Rango inválido o mayor a 62 días",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    List<AppointmentResponse> search(@Parameter(description = "Desde (yyyy-MM-dd)") LocalDate from,
                                     @Parameter(description = "Hasta (yyyy-MM-dd)") LocalDate to,
                                     @Parameter(description = "Filtrar por estado") AppointmentStatus status);

    @Operation(summary = "Horarios libres de un día",
            description = "Aplica horarios de atención, bloqueos y turnos tomados, sin el tope de 60 días de la página "
                    + "pública. `excludeAppointmentId` libera el horario de ese turno, para reprogramarlo.")
    @ApiResponse(responseCode = "200", description = "Horas de inicio libres")
    @ApiResponse(responseCode = "404", description = "El servicio no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El servicio no está disponible",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    DayAvailabilityResponse availability(@Parameter(description = "Id del servicio") Long serviceId,
                                         @Parameter(description = "Día (yyyy-MM-dd)") LocalDate date,
                                         @Parameter(description = "Turno a ignorar") Long excludeAppointmentId);

    @Operation(summary = "Ver un turno")
    @ApiResponse(responseCode = "200", description = "Turno")
    @ApiResponse(responseCode = "404", description = "El turno no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AppointmentResponse get(@Parameter(description = "Id del turno") Long id);

    @Operation(summary = "Cargar turno",
            description = "Puede quedar fuera del horario de atención o de un bloqueo, pero nunca superpuesto a otro "
                    + "turno ni en el pasado. Con `clientId`, el nombre y el teléfono que falten se toman del cliente.")
    @ApiResponse(responseCode = "201", description = "Turno confirmado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, hora fuera de la grilla o en el pasado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El servicio o el cliente no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Horario ocupado o servicio no disponible",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AppointmentResponse create(AppointmentRequest request);

    @Operation(summary = "Reprogramar o corregir turno", description = "Solo turnos confirmados. Libera el horario "
            + "anterior.")
    @ApiResponse(responseCode = "200", description = "Turno modificado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, hora fuera de la grilla o en el pasado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El turno, el servicio o el cliente no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "Turno no confirmado, horario ocupado o servicio no disponible",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AppointmentResponse update(@Parameter(description = "Id del turno") Long id, AppointmentRequest request);

    @Operation(summary = "Cambiar estado",
            description = "Solo desde CONFIRMED. CANCELLED y NO_SHOW liberan el horario; ATTENDED exige cliente.")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El turno no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El turno ya no está confirmado o le falta el cliente",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AppointmentResponse changeStatus(@Parameter(description = "Id del turno") Long id, AppointmentStatusRequest request);

    @Operation(summary = "Vincular con un cliente", description = "Solo turnos confirmados.")
    @ApiResponse(responseCode = "200", description = "Turno vinculado")
    @ApiResponse(responseCode = "404", description = "El turno o el cliente no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El turno ya no está confirmado",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    AppointmentResponse linkClient(@Parameter(description = "Id del turno") Long id, AppointmentClientRequest request);

    @Operation(summary = "Enviar aviso por email a la clienta",
            description = "Solo cuando la responsable lo pide (ADR 016). BOOKED y RESCHEDULED para turnos confirmados, "
                    + "CANCELLED para cancelados. El turno no cambia aunque el envío falle.")
    @ApiResponse(responseCode = "204", description = "Email enviado")
    @ApiResponse(responseCode = "400", description = "El turno no tiene email o el tipo de aviso es inválido",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "El turno no existe",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "El aviso no corresponde al estado del turno",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "502", description = "No se pudo enviar el email",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    void sendNotice(@Parameter(description = "Id del turno") Long id, AppointmentNoticeRequest request);
}
