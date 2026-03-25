package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.util.UUID;

public record ManufacturedAdminResponse(
        UUID id,
        String sku,
        String name,
        String unit,
        String purchaseUnit,
        int purchaseToStockRatio,
        String imageUrl,
        String description,
        int quantityOnHand,
        int reorderLevel,
        BigDecimal latestUnitCost,
        BigDecimal latestPurchaseUnitCost,
        boolean lowStock,
        boolean active
) {
}
