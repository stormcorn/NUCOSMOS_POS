package com.nucosmos.pos.backend.report;

import java.math.BigDecimal;

public record SalesTrendPointResponse(
        String bucketLabel,
        String bucketStart,
        int orderCount,
        BigDecimal grossSalesAmount,
        BigDecimal refundedAmount,
        BigDecimal netSalesAmount
) {
}
