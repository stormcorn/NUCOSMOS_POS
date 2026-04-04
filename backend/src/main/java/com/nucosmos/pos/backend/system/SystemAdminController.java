package com.nucosmos.pos.backend.system;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/system")
@PreAuthorize("hasRole('ADMIN')")
public class SystemAdminController {

    private final DockerMaintenanceService dockerMaintenanceService;

    public SystemAdminController(DockerMaintenanceService dockerMaintenanceService) {
        this.dockerMaintenanceService = dockerMaintenanceService;
    }

    @GetMapping("/docker-cache")
    public ApiResponse<DockerMaintenanceStatusResponse> dockerCacheStatus() {
        return ApiResponse.ok(dockerMaintenanceService.getStatus());
    }

    @PostMapping("/docker-cache/cleanup")
    public ApiResponse<DockerMaintenanceCleanupResponse> cleanupDockerCache() {
        return ApiResponse.ok(dockerMaintenanceService.cleanupCaches());
    }
}
