package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        boolean testOrder,
        String status,
        String paymentStatus,
        String storeCode,
        String deviceCode,
        String createdByEmployeeCode,
        int itemCount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal refundedAmount,
        BigDecimal cogsAmount,
        BigDecimal netCogsAmount,
        BigDecimal grossProfitAmount,
        OffsetDateTime orderedAt,
        OffsetDateTime closedAt
) {
}
