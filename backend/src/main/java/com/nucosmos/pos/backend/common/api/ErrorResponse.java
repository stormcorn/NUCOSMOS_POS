package com.nucosmos.pos.backend.common.api;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        boolean success,
        String message,
        List<String> details,
        Instant timestamp
) {
    public static ErrorResponse of(String message, List<String> details) {
        return new ErrorResponse(false, message, details, Instant.now());
    }
}
