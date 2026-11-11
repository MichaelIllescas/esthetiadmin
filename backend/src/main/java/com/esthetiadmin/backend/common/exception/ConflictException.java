package com.esthetiadmin.backend.common.exception;

/** The operation violates a business rule, such as a duplicate name or a record still in use (HTTP 409). */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(message);
    }
}
