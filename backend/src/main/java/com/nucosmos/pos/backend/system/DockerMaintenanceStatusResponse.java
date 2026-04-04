package com.nucosmos.pos.backend.system;

public record DockerMaintenanceStatusResponse(
        boolean enabled,
        boolean available,
        String dockerBinaryPath,
        String dockerSocketPath,
        String summary,
        String details
) {
}
