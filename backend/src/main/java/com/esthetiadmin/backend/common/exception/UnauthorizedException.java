package com.esthetiadmin.backend.common.exception;

/** The credentials are wrong or missing (HTTP 401). */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
