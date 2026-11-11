package com.esthetiadmin.backend.common.exception;

/** The same client sent too many requests in a short time (HTTP 429, ADR 015). */
public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
