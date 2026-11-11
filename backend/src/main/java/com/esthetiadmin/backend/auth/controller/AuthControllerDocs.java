package com.esthetiadmin.backend.auth.controller;

import com.esthetiadmin.backend.auth.dto.LoginRequest;
import com.esthetiadmin.backend.auth.dto.LoginResponse;
import com.esthetiadmin.backend.common.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Autenticación", description = "Inicio de sesión y emisión del token JWT")
public interface AuthControllerDocs {

    @SecurityRequirements
    @Operation(
            summary = "Iniciar sesión",
            description = "Verifica usuario y contraseña y devuelve un token JWT para usar como Bearer en el resto de la API."
    )
    @ApiResponse(responseCode = "200", description = "Login correcto")
    @ApiResponse(responseCode = "400", description = "Usuario o contraseña vacíos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos desde la misma IP (ADR 015)",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    LoginResponse login(LoginRequest request, @Parameter(hidden = true) HttpServletRequest httpRequest);
}
