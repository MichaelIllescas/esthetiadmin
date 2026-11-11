package com.esthetiadmin.backend.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** An appointment loaded from the panel. With a client, her name, phone and email fill whatever is left empty. */
@Schema(description = "Datos para cargar o modificar un turno desde el panel")
public record AppointmentRequest(
        @Schema(description = "Id del servicio", example = "10")
        @NotNull(message = "El servicio es obligatorio") Long serviceId,
        @Schema(description = "Inicio del turno, en punto o y media", example = "2026-10-08T10:30:00")
        @NotNull(message = "El día y la hora del turno son obligatorios") LocalDateTime startsAt,
        @Schema(description = "Id del cliente, si el turno es de un cliente ya registrado", example = "7", nullable = true)
        Long clientId,
        @Schema(description = "Nombre; si se omite, el del cliente", example = "Carina")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String firstName,
        @Schema(description = "Apellido; si se omite, el del cliente", example = "Diaz")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres") String lastName,
        @Schema(description = "Teléfono; si se omite, el del cliente. Se guardan solo sus números (8 a 15)",
                example = "341 555-1234")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres") String phone,
        @Schema(description = "Observaciones opcionales", example = "Primera vez")
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres") String notes,
        @Schema(description = "Email opcional, solo para avisos del turno; si se omite, el del cliente",
                example = "carina@mail.com")
        @Email(message = "El email no es válido")
        @Size(max = 254, message = "El email no puede superar los 254 caracteres") String email) {
}
