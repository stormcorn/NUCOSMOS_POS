package com.nucosmos.pos.backend.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ProductUpsertRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 500) String imageUrl,
        @NotNull @DecimalMin(value = "0.01") BigDecimal price,
        boolean campaignEnabled,
        @Size(max = 80) String campaignLabel,
        @DecimalMin(value = "0.01") BigDecimal campaignPrice,
        OffsetDateTime campaignStartsAt,
        OffsetDateTime campaignEndsAt,
        @Size(max = 255) String recipeNote,
        List<ProductMaterialComponentRequest> materialComponents,
        List<ProductPackagingComponentRequest> packagingComponents
) {
}
