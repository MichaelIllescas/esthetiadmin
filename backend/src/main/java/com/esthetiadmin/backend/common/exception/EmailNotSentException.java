package com.esthetiadmin.backend.common.exception;

/** The mail server refused or did not answer (HTTP 502, ADR 016). Whatever was saved before stays saved. */
public class EmailNotSentException extends BusinessException {

    public EmailNotSentException(Throwable cause) {
        super("No se pudo enviar el email. Probá de nuevo más tarde");
        initCause(cause);
    }
}
