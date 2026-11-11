package com.esthetiadmin.backend.auth.security;

import com.esthetiadmin.backend.common.exception.ApiError;
import com.esthetiadmin.backend.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/**
 * Writes security failures (401 and 403) with the same JSON format as the rest of the API.
 * These errors happen inside the filter chain, so the controller advice never sees them.
 */
@Component
@RequiredArgsConstructor
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                "Tu sesión no es válida o expiró. Iniciá sesión nuevamente");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                "No tenés permisos para realizar esta acción");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, ErrorCode code, String message) throws IOException {
        ApiError body = new ApiError(status.value(), code, message, request.getRequestURI(), OffsetDateTime.now());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
