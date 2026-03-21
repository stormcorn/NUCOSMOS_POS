package com.nucosmos.pos.backend.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record InventoryMovementRequest(
        @NotNull UUID productId,
        @NotBlank String movementType,
        @NotNull @Min(1) Integer quantity,
        BigDecimal unitCost,
        @Size(max = 255) String note
) {
}
