package com.esthetiadmin.backend.appointment.dto;

import com.esthetiadmin.backend.appointment.entity.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Nuevo estado del turno")
public record AppointmentStatusRequest(
        @Schema(description = "CANCELLED, ATTENDED o NO_SHOW", example = "ATTENDED")
        @NotNull(message = "El estado es obligatorio") AppointmentStatus status) {
}
