package com.nucosmos.pos.backend.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductAdminResponse(
        UUID id,
        UUID categoryId,
        String categoryCode,
        String categoryName,
        String sku,
        String name,
        String description,
        String imageUrl,
        BigDecimal price,
        boolean active,
        List<ProductMaterialComponentResponse> materialComponents,
        List<ProductPackagingComponentResponse> packagingComponents,
        List<ProductRecipeVersionSummaryResponse> recipeVersions,
        BigDecimal materialCost,
        BigDecimal packagingCost,
        BigDecimal totalCost
) {
}
