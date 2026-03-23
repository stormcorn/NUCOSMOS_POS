package com.nucosmos.pos.backend.inventory;

import java.util.UUID;

public record InventoryStockResponse(
        UUID productId,
        String sku,
        String name,
        String categoryName,
        String imageUrl,
        int sellableQuantity,
        int defectiveQuantity,
        int quantityOnHand,
        int reorderLevel,
        boolean lowStock
) {
}
