package com.nucosmos.pos.backend.report;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderProfitabilityResponse(
        UUID orderId,
        String orderNumber,
        OffsetDateTime orderedAt,
        int itemCount,
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
