package com.esthetiadmin.backend.appointment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Pedido de turno desde la página pública")
public record PublicBookingRequest(
        @Schema(description = "Id del servicio", example = "10")
        @NotNull(message = "Elegí un servicio") Long serviceId,
        @Schema(description = "Inicio del turno, uno de los horarios libres", example = "2026-10-08T10:30:00")
        @NotNull(message = "Elegí el día y la hora") LocalDateTime startsAt,
        @Schema(description = "Nombre", example = "Carina")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String firstName,
        @Schema(description = "Apellido", example = "Diaz")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres") String lastName,
        @Schema(description = "Teléfono; se guardan solo sus números (8 a 15)", example = "341 555-1234")
        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres") String phone,
        @Schema(description = "Email opcional, solo para recibir la confirmación", example = "carina@mail.com")
        @Email(message = "El email no es válido")
        @Size(max = 254, message = "El email no puede superar los 254 caracteres") String email) {
}
