package com.nucosmos.pos.backend.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductCustomizationOptionRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull @DecimalMin(value = "0.00") BigDecimal priceDelta,
        boolean defaultSelected,
        @Min(0) int displayOrder
) {
}
