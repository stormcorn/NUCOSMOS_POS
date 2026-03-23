package com.nucosmos.pos.backend.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductMaterialComponentRequest(
        @NotNull UUID materialItemId,
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantity
) {
}
