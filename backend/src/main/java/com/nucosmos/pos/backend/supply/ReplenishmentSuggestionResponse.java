package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.util.UUID;

public record ReplenishmentSuggestionResponse(
        String itemType,
        UUID itemId,
        String sku,
        String name,
        String stockUnit,
        String purchaseUnit,
        int purchaseToStockRatio,
        int quantityOnHand,
        int reorderLevel,
        int suggestedOrderQuantity,
        BigDecimal latestUnitCost,
        BigDecimal latestPurchaseUnitCost,
        BigDecimal estimatedOrderCost
) {
}
