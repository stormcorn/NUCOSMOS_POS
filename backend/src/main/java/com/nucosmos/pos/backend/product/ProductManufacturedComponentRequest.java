package com.nucosmos.pos.backend.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductManufacturedComponentRequest(
        @NotNull UUID manufacturedItemId,
        @NotNull @DecimalMin(value = "0.001") BigDecimal quantity
) {
}
