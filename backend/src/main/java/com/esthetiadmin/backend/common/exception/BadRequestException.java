package com.esthetiadmin.backend.common.exception;

/** The request is well formed but its values do not make sense together (HTTP 400). */
public class BadRequestException extends BusinessException {

    public BadRequestException(String message) {
        super(message);
    }
}
