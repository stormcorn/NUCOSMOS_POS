package com.nucosmos.pos.backend.supply;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class SupplyAdminController {

    private final SupplyAdminService supplyAdminService;

    public SupplyAdminController(SupplyAdminService supplyAdminService) {
        this.supplyAdminService = supplyAdminService;
    }

    @GetMapping("/materials")
    public ApiResponse<List<MaterialAdminResponse>> listMaterials(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listMaterials(currentUser(authentication)));
    }

    @GetMapping("/materials/movements")
    public ApiResponse<List<MaterialMovementResponse>> listMaterialMovements(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listMaterialMovements(currentUser(authentication)));
    }

    @PostMapping("/materials")
    public ApiResponse<MaterialAdminResponse> createMaterial(
            Authentication authentication,
            @Valid @RequestBody MaterialUpsertRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.createMaterial(currentUser(authentication), request));
    }

    @PutMapping("/materials/{materialId}")
    public ApiResponse<MaterialAdminResponse> updateMaterial(
            Authentication authentication,
            @PathVariable UUID materialId,
            @Valid @RequestBody MaterialUpsertRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.updateMaterial(currentUser(authentication), materialId, request));
    }

    @PostMapping("/materials/{materialId}/deactivate")
    public ApiResponse<MaterialAdminResponse> deactivateMaterial(
            Authentication authentication,
            @PathVariable UUID materialId
    ) {
        return ApiResponse.ok(supplyAdminService.deactivateMaterial(currentUser(authentication), materialId));
    }

    @PostMapping("/materials/{materialId}/movements")
    public ApiResponse<MaterialMovementResponse> createMaterialMovement(
            Authentication authentication,
            @PathVariable UUID materialId,
            @Valid @RequestBody MaterialMovementRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.createMaterialMovement(currentUser(authentication), materialId, request));
    }

    @GetMapping("/packaging-items")
    public ApiResponse<List<PackagingAdminResponse>> listPackagingItems(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listPackagingItems(currentUser(authentication)));
    }

    @GetMapping("/packaging-items/movements")
    public ApiResponse<List<PackagingMovementResponse>> listPackagingMovements(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listPackagingMovements(currentUser(authentication)));
    }

    @PostMapping("/packaging-items")
    public ApiResponse<PackagingAdminResponse> createPackagingItem(
            Authentication authentication,
            @Valid @RequestBody PackagingUpsertRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.createPackagingItem(currentUser(authentication), request));
    }

    @PutMapping("/packaging-items/{packagingItemId}")
    public ApiResponse<PackagingAdminResponse> updatePackagingItem(
            Authentication authentication,
            @PathVariable UUID packagingItemId,
            @Valid @RequestBody PackagingUpsertRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.updatePackagingItem(currentUser(authentication), packagingItemId, request));
    }

    @PostMapping("/packaging-items/{packagingItemId}/deactivate")
    public ApiResponse<PackagingAdminResponse> deactivatePackagingItem(
            Authentication authentication,
            @PathVariable UUID packagingItemId
    ) {
        return ApiResponse.ok(supplyAdminService.deactivatePackagingItem(currentUser(authentication), packagingItemId));
    }

    @PostMapping("/packaging-items/{packagingItemId}/movements")
    public ApiResponse<PackagingMovementResponse> createPackagingMovement(
            Authentication authentication,
            @PathVariable UUID packagingItemId,
            @Valid @RequestBody PackagingMovementRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.createPackagingMovement(currentUser(authentication), packagingItemId, request));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
