package com.nucosmos.pos.backend.report;

import java.math.BigDecimal;

public record CostTransferSummaryResponse(
        BigDecimal grossSalesAmount,
        BigDecimal refundedAmount,
        BigDecimal netSalesAmount,
        BigDecimal realizedCogsAmount,
        BigDecimal realizedRefundedCogsAmount,
        BigDecimal realizedNetCogsAmount,
        BigDecimal standardCogsAmount,
        BigDecimal standardRefundedCogsAmount,
        BigDecimal standardNetCogsAmount,
        BigDecimal cogsVarianceAmount,
        BigDecimal cogsVarianceRate,
        BigDecimal realizedGrossProfitAmount,
        BigDecimal standardGrossProfitAmount,
        BigDecimal realizedGrossMarginRate,
        BigDecimal standardGrossMarginRate
) {
}
