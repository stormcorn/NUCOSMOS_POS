package com.nucosmos.pos.backend.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryStockLevelUpdateRequest(
        @NotNull @Min(0) Integer reorderLevel
) {
}
