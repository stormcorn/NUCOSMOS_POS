package com.nucosmos.pos.backend.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductPackagingComponentRequest(
        @NotNull UUID packagingItemId,
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantity
) {
}
