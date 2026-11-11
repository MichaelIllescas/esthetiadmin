package com.esthetiadmin.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Token JWT emitido tras un login correcto")
public record LoginResponse(
        @Schema(description = "Token JWT para enviar en el header Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.firma")
        String token,
        @Schema(description = "Momento en que el token vence (UTC)", example = "2026-09-29T16:30:45Z")
        Instant expiresAt
) {
}
