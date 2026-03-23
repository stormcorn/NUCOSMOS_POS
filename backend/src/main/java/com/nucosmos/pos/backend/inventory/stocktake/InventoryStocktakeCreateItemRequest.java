package com.nucosmos.pos.backend.inventory.stocktake;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record InventoryStocktakeCreateItemRequest(
        @NotNull UUID productId,
        @Min(0) int countedSellableQuantity,
        @Size(max = 50) String reasonCode,
        @Size(max = 255) String note
) {
}
