package com.nucosmos.pos.backend.inventory.stocktake;

import java.util.UUID;

public record InventoryStocktakeItemResponse(
        UUID id,
        UUID productId,
        String productSku,
        String productName,
        String categoryName,
        int expectedSellableQuantity,
        int countedSellableQuantity,
        int varianceQuantity,
        String reasonCode,
        String note
) {
}
