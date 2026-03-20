package com.nucosmos.pos.backend.system;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemInfoController {

    private final String appName;

    public SystemInfoController(@Value("${spring.application.name}") String appName) {
        this.appName = appName;
    }

    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        return ApiResponse.ok(Map.of(
                "applicationName", appName,
                "environment", "local",
                "serverTime", Instant.now(),
                "javaVersion", System.getProperty("java.version")
        ));
    }
}
