package com.nucosmos.pos.backend.report;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductProfitabilityResponse(
        UUID productId,
        String sku,
        String name,
        String categoryName,
        int soldQuantity,
        int refundedQuantity,
        int netQuantity,
        BigDecimal netSalesAmount,
        BigDecimal realizedNetCogsAmount,
        BigDecimal standardNetCogsAmount,
        BigDecimal cogsVarianceAmount,
        BigDecimal realizedGrossProfitAmount,
        BigDecimal standardGrossProfitAmount,
        BigDecimal realizedGrossMarginRate,
        BigDecimal standardGrossMarginRate
) {
}
