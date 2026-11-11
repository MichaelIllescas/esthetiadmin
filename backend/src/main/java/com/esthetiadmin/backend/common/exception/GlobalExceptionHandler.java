package com.esthetiadmin.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String VALIDATION_SEPARATOR = "; ";

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ErrorCode.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(EmailNotSentException.class)
    public ResponseEntity<ApiError> handleEmailNotSent(EmailNotSentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_GATEWAY, ErrorCode.EMAIL_NOT_SENT, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(VALIDATION_SEPARATOR));
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .distinct()
                .collect(Collectors.joining(VALIDATION_SEPARATOR));
        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "El cuerpo de la petición no es válido", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "El valor del parámetro '" + ex.getName() + "' no es válido";
        return build(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message, request);
    }

    /** Safety net: the database rejected the operation (foreign key or unique constraint). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}", request.getRequestURI(), ex);
        String message = "La operación no se puede completar porque el registro ya existe o está siendo utilizado";
        return build(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) throws Exception {
        // Security exceptions are translated to 401/403 by Spring Security, not here.
        if (ex instanceof AccessDeniedException || ex instanceof AuthenticationException) {
            throw ex;
        }
        if (ex instanceof ErrorResponse frameworkError && !frameworkError.getStatusCode().is5xxServerError()) {
            HttpStatus status = HttpStatus.valueOf(frameworkError.getStatusCode().value());
            return build(status, codeFor(status), messageFor(status), request);
        }
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR,
                "Ocurrió un error inesperado. Intentá nuevamente más tarde", request);
    }

    private ErrorCode codeFor(HttpStatus status) {
        return status == HttpStatus.NOT_FOUND ? ErrorCode.NOT_FOUND : ErrorCode.BAD_REQUEST;
    }

    private String messageFor(HttpStatus status) {
        return status == HttpStatus.NOT_FOUND ? "El recurso solicitado no existe" : "La petición no es válida";
    }

    private ResponseEntity<ApiError> build(HttpStatus status, ErrorCode code, String message, HttpServletRequest request) {
        ApiError body = new ApiError(status.value(), code, message, request.getRequestURI(), OffsetDateTime.now());
        return ResponseEntity.status(status).body(body);
    }
}
