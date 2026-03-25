package com.nucosmos.pos.backend.supply;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PackagingUpsertRequest(
        @NotBlank @Size(max = 50) String sku,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 30) String unit,
        @NotBlank @Size(max = 30) String purchaseUnit,
        @NotNull @Min(1) Integer purchaseToStockRatio,
        @Size(max = 120) String specification,
        String imageUrl,
        @Size(max = 500) String description,
        @NotNull @Min(0) Integer reorderLevel,
        @DecimalMin("0.0") BigDecimal latestUnitCost
) {
}
