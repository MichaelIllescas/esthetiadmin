package com.esthetiadmin.backend.settings.controller;

import com.esthetiadmin.backend.settings.dto.PublicContactResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Contacto público", description = "Sin login: el celular del centro para cancelar o reagendar")
public interface PublicContactControllerDocs {

    @Operation(summary = "Celular de contacto", description = "Nunca devuelve el email de avisos.")
    @ApiResponse(responseCode = "200", description = "Celular, o null si no está cargado")
    PublicContactResponse get();
}
