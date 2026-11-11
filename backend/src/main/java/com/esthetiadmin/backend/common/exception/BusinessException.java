package com.esthetiadmin.backend.common.exception;

/**
 * Base for expected errors raised by business rules. The message is shown as-is to the user,
 * so it must be written in Spanish and must not contain internal details.
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
