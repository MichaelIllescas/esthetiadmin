package com.esthetiadmin.backend.settings.controller;

import com.esthetiadmin.backend.common.exception.ApiError;
import com.esthetiadmin.backend.settings.dto.SettingsRequest;
import com.esthetiadmin.backend.settings.dto.SettingsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Configuración", description = "Datos de contacto del negocio")
public interface SettingsControllerDocs {

    @Operation(summary = "Ver la configuración")
    @ApiResponse(responseCode = "200", description = "Email de avisos y celular de contacto")
    SettingsResponse get();

    @Operation(summary = "Guardar la configuración",
            description = "Los dos campos son opcionales; null o vacío los borra. El celular se guarda solo con "
                    + "sus dígitos y el email en minúsculas.")
    @ApiResponse(responseCode = "200", description = "Configuración guardada")
    @ApiResponse(responseCode = "400", description = "Email inválido o celular sin 8 a 15 dígitos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    SettingsResponse update(SettingsRequest request);
}
