package com.esthetiadmin.backend.availability.dto;

import com.esthetiadmin.backend.availability.entity.AvailabilityBlock;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Bloqueo de la agenda")
public record AvailabilityBlockResponse(
        @Schema(description = "Id del bloqueo", example = "3") Long id,
        @Schema(description = "Primer día bloqueado", example = "2027-01-04") LocalDate startDate,
        @Schema(description = "Último día bloqueado", example = "2027-01-15") LocalDate endDate,
        @Schema(description = "Hora de inicio; vacía si es el día completo", example = "15:00", type = "string",
                nullable = true)
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @Schema(description = "Hora de fin; vacía si es el día completo", example = "19:00", type = "string",
                nullable = true)
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        @Schema(description = "Motivo", example = "Feriado") String reason,
        @Schema(description = "Turnos activos que quedan dentro del bloqueo (solo al crearlo)")
        List<AffectedAppointmentResponse> affectedAppointments) {

    public static AvailabilityBlockResponse from(AvailabilityBlock block, List<AffectedAppointmentResponse> affected) {
        return new AvailabilityBlockResponse(block.getId(), block.getStartDate(), block.getEndDate(), block.getStartTime(),
                block.getEndTime(), block.getReason(), affected);
    }
}
