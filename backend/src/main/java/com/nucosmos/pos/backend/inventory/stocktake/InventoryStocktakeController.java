package com.nucosmos.pos.backend.inventory.stocktake;

import com.nucosmos.pos.backend.auth.AuthenticatedUser;
import com.nucosmos.pos.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/inventory/stocktakes")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class InventoryStocktakeController {

    private final InventoryStocktakeService inventoryStocktakeService;

    public InventoryStocktakeController(InventoryStocktakeService inventoryStocktakeService) {
        this.inventoryStocktakeService = inventoryStocktakeService;
    }

    @GetMapping
    public ApiResponse<List<InventoryStocktakeResponse>> listStocktakes(Authentication authentication) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(inventoryStocktakeService.listStocktakes(user));
    }

    @PostMapping
    public ApiResponse<InventoryStocktakeResponse> createStocktake(
            Authentication authentication,
            @Valid @RequestBody InventoryStocktakeCreateRequest request
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(inventoryStocktakeService.createStocktake(user, request));
    }
}
