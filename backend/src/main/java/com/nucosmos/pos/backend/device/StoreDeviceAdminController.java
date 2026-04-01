package com.nucosmos.pos.backend.device;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import com.nucosmos.pos.backend.store.StoreReceiptSettingsRequest;
import com.nucosmos.pos.backend.store.StoreReceiptSettingsResponse;
import com.nucosmos.pos.backend.store.StoreSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StoreDeviceAdminController {

    private final StoreDeviceService storeDeviceService;

    public StoreDeviceAdminController(StoreDeviceService storeDeviceService) {
        this.storeDeviceService = storeDeviceService;
    }

    @GetMapping("/api/v1/admin/stores")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<List<StoreSummaryResponse>> listStores() {
        return ApiResponse.ok(storeDeviceService.listStores());
    }

    @GetMapping("/api/v1/admin/stores/{storeId}/receipt-settings")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<StoreReceiptSettingsResponse> getReceiptSettings(@PathVariable java.util.UUID storeId) {
        return ApiResponse.ok(storeDeviceService.getReceiptSettings(storeId));
    }

    @PutMapping("/api/v1/admin/stores/{storeId}/receipt-settings")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<StoreReceiptSettingsResponse> updateReceiptSettings(
            @PathVariable java.util.UUID storeId,
            @Valid @RequestBody StoreReceiptSettingsRequest request
    ) {
        return ApiResponse.ok(storeDeviceService.updateReceiptSettings(storeId, request));
    }

    @GetMapping("/api/v1/admin/devices")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ApiResponse<List<DeviceAdminResponse>> listDevices(
            @RequestParam(required = false) String storeCode,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(storeDeviceService.listDevices(storeCode, status));
    }

    @PostMapping("/api/v1/devices/heartbeat")
    public ApiResponse<DeviceHeartbeatResponse> heartbeat(
            Authentication authentication,
            @Valid @RequestBody(required = false) DeviceHeartbeatRequest request
    ) {
        return ApiResponse.ok(storeDeviceService.recordHeartbeat(authentication, request));
    }
}
