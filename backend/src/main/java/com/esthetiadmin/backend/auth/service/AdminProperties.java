package com.esthetiadmin.backend.auth.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        @NotBlank @Size(max = 50) String username,
        @NotBlank @Size(min = 8, message = "ADMIN_PASSWORD must have at least 8 characters") String password
) {

    /** Keeps the password out of logs. */
    @Override
    public String toString() {
        return "AdminProperties[username=" + username + ", password=***]";
    }
}
