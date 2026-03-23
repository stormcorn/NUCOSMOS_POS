package com.nucosmos.pos.backend.report;

import java.math.BigDecimal;

public record CategoryProfitabilityResponse(
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
