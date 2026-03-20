package com.nucosmos.pos.backend.sync;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping("/bootstrap")
    public ApiResponse<SyncBootstrapResponse> bootstrap(Authentication authentication) {
        return ApiResponse.ok(syncService.bootstrap(authentication));
    }

    @GetMapping("/catalog")
    public ApiResponse<SyncCatalogResponse> catalog(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime since
    ) {
        return ApiResponse.ok(syncService.catalog(authentication, since));
    }
}
