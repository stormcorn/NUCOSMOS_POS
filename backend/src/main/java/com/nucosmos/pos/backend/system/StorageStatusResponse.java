package com.nucosmos.pos.backend.system;

import java.time.Instant;

public record StorageStatusResponse(
        String monitoredPath,
        long totalBytes,
        long usableBytes,
        long usedBytes,
        double freePercent,
        String level,
        double warningThresholdPercent,
        double criticalThresholdPercent,
        String message,
        Instant checkedAt
) {
}
