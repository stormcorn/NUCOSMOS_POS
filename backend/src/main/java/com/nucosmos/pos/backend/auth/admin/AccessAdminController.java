package com.nucosmos.pos.backend.auth.admin;

import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/access")
@PreAuthorize("hasRole('ADMIN')")
public class AccessAdminController {

    private final AccessAdminService accessAdminService;

    public AccessAdminController(AccessAdminService accessAdminService) {
        this.accessAdminService = accessAdminService;
    }

    @GetMapping("/users")
    public ApiResponse<List<UserAdminResponse>> users(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String storeCode
    ) {
        return ApiResponse.ok(accessAdminService.listUsers(status, storeCode));
    }

    @PostMapping("/users")
    public ApiResponse<UserAdminResponse> createUser(@Valid @RequestBody UserAdminRequest request) {
        return ApiResponse.ok(accessAdminService.createUser(request));
    }

    @PutMapping("/users/{userId}")
    public ApiResponse<UserAdminResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserAdminRequest request
    ) {
        return ApiResponse.ok(accessAdminService.updateUser(userId, request));
    }

    @PostMapping("/phone-registrations/clear-pending")
    public ApiResponse<ClearPendingPhoneRegistrationResponse> clearPendingPhoneRegistrations(
            @Valid @RequestBody ClearPendingPhoneRegistrationRequest request
    ) {
        return ApiResponse.ok(accessAdminService.clearPendingPhoneRegistrations(request));
    }

    @GetMapping("/roles")
    public ApiResponse<List<RoleAdminResponse>> roles() {
        return ApiResponse.ok(accessAdminService.listRoles());
    }

    @PostMapping("/roles")
    public ApiResponse<RoleAdminResponse> createRole(@Valid @RequestBody RoleAdminRequest request) {
        return ApiResponse.ok(accessAdminService.createRole(request));
    }

    @PutMapping("/roles/{roleId}")
    public ApiResponse<RoleAdminResponse> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody RoleAdminRequest request
    ) {
        return ApiResponse.ok(accessAdminService.updateRole(roleId, request));
    }

    @GetMapping("/permissions")
    public ApiResponse<List<PermissionDefinitionResponse>> permissions() {
        return ApiResponse.ok(accessAdminService.listPermissionCatalog());
    }
}
