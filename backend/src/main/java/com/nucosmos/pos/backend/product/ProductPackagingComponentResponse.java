package com.nucosmos.pos.backend.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductPackagingComponentResponse(
        UUID packagingItemId,
        String sku,
        String name,
        String unit,
        String specification,
        BigDecimal quantity,
        BigDecimal latestUnitCost,
        BigDecimal lineCost
) {
}
