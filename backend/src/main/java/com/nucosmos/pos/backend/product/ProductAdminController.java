package com.nucosmos.pos.backend.product;

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
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    public ProductAdminController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping("/product-categories")
    public ApiResponse<List<ProductCategorySummaryResponse>> listProductCategories() {
        return ApiResponse.ok(productAdminService.listProductCategories());
    }

    @PostMapping("/product-categories")
    public ApiResponse<ProductCategorySummaryResponse> createProductCategory(
            @Valid @RequestBody ProductCategoryUpsertRequest request
    ) {
        return ApiResponse.ok(productAdminService.createProductCategory(request));
    }

    @PutMapping("/product-categories/{categoryId}")
    public ApiResponse<ProductCategorySummaryResponse> updateProductCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody ProductCategoryUpsertRequest request
    ) {
        return ApiResponse.ok(productAdminService.updateProductCategory(categoryId, request));
    }

    @PostMapping("/product-categories/{categoryId}/deactivate")
    public ApiResponse<ProductCategorySummaryResponse> deactivateProductCategory(@PathVariable UUID categoryId) {
        return ApiResponse.ok(productAdminService.deactivateProductCategory(categoryId));
    }

    @GetMapping("/products")
    public ApiResponse<List<ProductAdminResponse>> listProducts(
            Authentication authentication,
            @RequestParam(required = false) Boolean active
    ) {
        return ApiResponse.ok(productAdminService.listProducts(currentUser(authentication), active));
    }

    @PostMapping("/products")
    public ApiResponse<ProductAdminResponse> createProduct(
            Authentication authentication,
            @Valid @RequestBody ProductUpsertRequest request
    ) {
        return ApiResponse.ok(productAdminService.createProduct(currentUser(authentication), request));
    }

    @PutMapping("/products/{productId}")
    public ApiResponse<ProductAdminResponse> updateProduct(
            Authentication authentication,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductUpsertRequest request
    ) {
        return ApiResponse.ok(productAdminService.updateProduct(currentUser(authentication), productId, request));
    }

    @PostMapping("/products/{productId}/deactivate")
    public ApiResponse<ProductAdminResponse> deactivateProduct(@PathVariable UUID productId) {
        return ApiResponse.ok(productAdminService.deactivateProduct(productId));
    }

    private AuthenticatedUser currentUser(Authentication authentication) {
        return (AuthenticatedUser) authentication.getPrincipal();
    }
}
