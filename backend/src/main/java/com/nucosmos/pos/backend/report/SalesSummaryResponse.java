package com.nucosmos.pos.backend.report;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SalesSummaryResponse(
        String storeCode,
        OffsetDateTime from,
        OffsetDateTime to,
        int orderCount,
        int voidedOrderCount,
        BigDecimal grossSalesAmount,
        BigDecimal refundedAmount,
        BigDecimal netSalesAmount,
        BigDecimal cashSalesAmount,
        BigDecimal cardSalesAmount,
        BigDecimal averageOrderAmount,
        BigDecimal cogsAmount,
        BigDecimal refundedCogsAmount,
        BigDecimal netCogsAmount,
        BigDecimal grossProfitAmount,
        BigDecimal grossMarginRate
) {
}
