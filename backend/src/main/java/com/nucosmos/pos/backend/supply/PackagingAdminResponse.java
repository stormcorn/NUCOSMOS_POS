package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.util.UUID;

public record PackagingAdminResponse(
        UUID id,
        String sku,
        String name,
        String unit,
        String purchaseUnit,
        int purchaseToStockRatio,
        String specification,
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
