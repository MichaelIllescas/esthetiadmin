package com.esthetiadmin.backend.client.dto;

import com.esthetiadmin.backend.client.entity.Client;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.Period;

@Schema(description = "Datos de un cliente")
public record ClientResponse(
        @Schema(description = "Id del cliente", example = "5") Long id,
        @Schema(description = "Nombre", example = "Ana") String firstName,
        @Schema(description = "Apellido", example = "Gómez") String lastName,
        @Schema(description = "Teléfono", example = "1155551234") String phone,
        @Schema(description = "Fecha de nacimiento", example = "1990-10-05") LocalDate birthDate,
        @Schema(description = "Edad calculada a hoy; null si no hay fecha de nacimiento", example = "35") Integer age,
        @Schema(description = "Observaciones", example = "Piel sensible") String notes,
        @Schema(description = "Email, solo para avisos de turnos", example = "ana@mail.com") String email
) {

    /** The age is not stored: it is calculated from the birth date as of {@code today}. */
    public static ClientResponse from(Client client, LocalDate today) {
        Integer age = client.getBirthDate() == null ? null : Period.between(client.getBirthDate(), today).getYears();
        return new ClientResponse(client.getId(), client.getFirstName(), client.getLastName(), client.getPhone(),
                client.getBirthDate(), age, client.getNotes(), client.getEmail());
    }
}
