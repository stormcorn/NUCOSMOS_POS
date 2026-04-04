package com.nucosmos.pos.backend.system;

import java.time.Instant;

public record DockerMaintenanceCleanupResponse(
        boolean executed,
        String summary,
        String beforeDetails,
        String afterDetails,
        String cleanupLog,
        Instant executedAt
) {
}
