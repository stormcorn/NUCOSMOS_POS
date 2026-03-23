package com.nucosmos.pos.backend.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductMaterialComponentResponse(
        UUID materialItemId,
        String sku,
        String name,
        String unit,
        BigDecimal quantity,
        BigDecimal latestUnitCost,
        BigDecimal lineCost
) {
}
