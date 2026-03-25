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

    @GetMapping("/materials/lots")
    public ApiResponse<List<MaterialLotResponse>> listMaterialLots(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listMaterialLots(currentUser(authentication)));
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

    @GetMapping("/manufactured-items")
    public ApiResponse<List<ManufacturedAdminResponse>> listManufacturedItems(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listManufacturedItems(currentUser(authentication)));
    }

    @GetMapping("/manufactured-items/movements")
    public ApiResponse<List<ManufacturedMovementResponse>> listManufacturedMovements(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listManufacturedMovements(currentUser(authentication)));
    }

    @GetMapping("/manufactured-items/lots")
    public ApiResponse<List<ManufacturedLotResponse>> listManufacturedLots(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listManufacturedLots(currentUser(authentication)));
    }

    @PostMapping("/manufactured-items")
    public ApiResponse<ManufacturedAdminResponse> createManufacturedItem(
            Authentication authentication,
            @Valid @RequestBody ManufacturedUpsertRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.createManufacturedItem(currentUser(authentication), request));
    }

    @PutMapping("/manufactured-items/{manufacturedItemId}")
    public ApiResponse<ManufacturedAdminResponse> updateManufacturedItem(
            Authentication authentication,
            @PathVariable UUID manufacturedItemId,
            @Valid @RequestBody ManufacturedUpsertRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.updateManufacturedItem(currentUser(authentication), manufacturedItemId, request));
    }

    @PostMapping("/manufactured-items/{manufacturedItemId}/deactivate")
    public ApiResponse<ManufacturedAdminResponse> deactivateManufacturedItem(
            Authentication authentication,
            @PathVariable UUID manufacturedItemId
    ) {
        return ApiResponse.ok(supplyAdminService.deactivateManufacturedItem(currentUser(authentication), manufacturedItemId));
    }

    @PostMapping("/manufactured-items/{manufacturedItemId}/movements")
    public ApiResponse<ManufacturedMovementResponse> createManufacturedMovement(
            Authentication authentication,
            @PathVariable UUID manufacturedItemId,
            @Valid @RequestBody ManufacturedMovementRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.createManufacturedMovement(currentUser(authentication), manufacturedItemId, request));
    }

    @GetMapping("/packaging-items")
    public ApiResponse<List<PackagingAdminResponse>> listPackagingItems(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listPackagingItems(currentUser(authentication)));
    }

    @GetMapping("/packaging-items/movements")
    public ApiResponse<List<PackagingMovementResponse>> listPackagingMovements(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listPackagingMovements(currentUser(authentication)));
    }

    @GetMapping("/packaging-items/lots")
    public ApiResponse<List<PackagingLotResponse>> listPackagingLots(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listPackagingLots(currentUser(authentication)));
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

    @GetMapping("/suppliers")
    public ApiResponse<List<SupplierResponse>> listSuppliers(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listSuppliers(currentUser(authentication)));
    }

    @PostMapping("/suppliers")
    public ApiResponse<SupplierResponse> createSupplier(
            Authentication authentication,
            @Valid @RequestBody SupplierUpsertRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.createSupplier(currentUser(authentication), request));
    }

    @PutMapping("/suppliers/{supplierId}")
    public ApiResponse<SupplierResponse> updateSupplier(
            Authentication authentication,
            @PathVariable UUID supplierId,
            @Valid @RequestBody SupplierUpsertRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.updateSupplier(currentUser(authentication), supplierId, request));
    }

    @PostMapping("/suppliers/{supplierId}/deactivate")
    public ApiResponse<SupplierResponse> deactivateSupplier(
            Authentication authentication,
            @PathVariable UUID supplierId
    ) {
        return ApiResponse.ok(supplyAdminService.deactivateSupplier(currentUser(authentication), supplierId));
    }

    @GetMapping("/replenishment-suggestions")
    public ApiResponse<List<ReplenishmentSuggestionResponse>> listReplenishmentSuggestions(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listReplenishmentSuggestions(currentUser(authentication)));
    }

    @GetMapping("/purchase-orders")
    public ApiResponse<List<PurchaseOrderResponse>> listPurchaseOrders(Authentication authentication) {
        return ApiResponse.ok(supplyAdminService.listPurchaseOrders(currentUser(authentication)));
    }

    @PostMapping("/purchase-orders")
    public ApiResponse<PurchaseOrderResponse> createPurchaseOrder(
            Authentication authentication,
            @Valid @RequestBody PurchaseOrderCreateRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.createPurchaseOrder(currentUser(authentication), request));
    }

    @PostMapping("/purchase-orders/{purchaseOrderId}/receive")
    public ApiResponse<PurchaseOrderResponse> receivePurchaseOrder(
            Authentication authentication,
            @PathVariable UUID purchaseOrderId,
            @Valid @RequestBody PurchaseOrderReceiveRequest request
    ) {
        return ApiResponse.ok(supplyAdminService.receivePurchaseOrder(currentUser(authentication), purchaseOrderId, request));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
