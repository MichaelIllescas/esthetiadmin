package com.esthetiadmin.backend.common.exception;

/** The requested resource does not exist (HTTP 404). */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(message);
    }
}
