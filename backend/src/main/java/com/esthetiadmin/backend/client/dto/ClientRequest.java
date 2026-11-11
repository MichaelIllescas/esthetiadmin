package com.esthetiadmin.backend.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Datos para crear o modificar un cliente")
public record ClientRequest(
        @Schema(description = "Nombre", example = "Ana")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres") String firstName,
        @Schema(description = "Apellido", example = "Gómez")
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres") String lastName,
        @Schema(description = "Teléfono, opcional", example = "1155551234")
        @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres") String phone,
        @Schema(description = "Fecha de nacimiento, opcional, no futura", example = "1990-10-05")
        @PastOrPresent(message = "La fecha de nacimiento no puede ser futura") LocalDate birthDate,
        @Schema(description = "Observaciones, opcional", example = "Piel sensible")
        @Size(max = 2000, message = "Las observaciones no pueden superar los 2000 caracteres") String notes,
        @Schema(description = "Email, opcional; solo para avisos de turnos", example = "ana@mail.com")
        @Email(message = "El email no es válido")
        @Size(max = 254, message = "El email no puede superar los 254 caracteres") String email
) {
}
