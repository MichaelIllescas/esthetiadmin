package com.esthetiadmin.backend.auth.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32, message = "JWT_SECRET must have at least 32 characters") String secret,
        @Positive long expirationMinutes
) {
}
