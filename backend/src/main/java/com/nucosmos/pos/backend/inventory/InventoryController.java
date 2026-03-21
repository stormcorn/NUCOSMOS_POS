package com.nucosmos.pos.backend.inventory;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stocks")
    public ApiResponse<List<InventoryStockResponse>> listStocks(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean lowStockOnly
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(inventoryService.listStocks(user, lowStockOnly));
    }

    @GetMapping("/movements")
    public ApiResponse<List<InventoryMovementResponse>> listMovements(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(inventoryService.listMovements(user));
    }

    @PostMapping("/movements")
    public ApiResponse<InventoryMovementResponse> createMovement(
            Authentication authentication,
            @Valid @RequestBody InventoryMovementRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(inventoryService.createMovement(user, request));
    }

    @PutMapping("/stocks/{productId}/reorder-level")
    public ApiResponse<InventoryStockResponse> updateReorderLevel(
            Authentication authentication,
            @PathVariable UUID productId,
            @Valid @RequestBody InventoryStockLevelUpdateRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(inventoryService.updateReorderLevel(user, productId, request));
    }
}
