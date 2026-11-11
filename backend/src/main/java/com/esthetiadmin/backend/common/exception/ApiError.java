package com.esthetiadmin.backend.common.exception;

import java.time.OffsetDateTime;

public record ApiError(
        int status,
        ErrorCode code,
        String message,
        String path,
        OffsetDateTime timestamp
) {
}
