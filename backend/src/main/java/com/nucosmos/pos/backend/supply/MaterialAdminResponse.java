package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.util.UUID;

public record MaterialAdminResponse(
        UUID id,
        String sku,
        String name,
        String unit,
        String description,
        int quantityOnHand,
        int reorderLevel,
        BigDecimal latestUnitCost,
        boolean lowStock,
        boolean active
) {
}
