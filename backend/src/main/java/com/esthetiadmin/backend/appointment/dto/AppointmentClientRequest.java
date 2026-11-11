package com.esthetiadmin.backend.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Cliente con el que se vincula el turno")
public record AppointmentClientRequest(
        @Schema(description = "Id del cliente", example = "7")
        @NotNull(message = "El cliente es obligatorio") Long clientId) {
}
