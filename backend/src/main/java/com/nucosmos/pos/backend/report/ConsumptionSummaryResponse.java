package com.nucosmos.pos.backend.report;

import java.math.BigDecimal;

public record ConsumptionSummaryResponse(
        String scope,
        String sku,
        String name,
        String unit,
        int consumedQuantity,
        BigDecimal consumedCost
) {
}
