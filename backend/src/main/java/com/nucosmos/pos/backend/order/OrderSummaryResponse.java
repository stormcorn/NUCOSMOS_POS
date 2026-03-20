package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String orderNumber,
        String status,
        String paymentStatus,
        String storeCode,
        String deviceCode,
        String createdByEmployeeCode,
        int itemCount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal refundedAmount,
        OffsetDateTime orderedAt,
        OffsetDateTime closedAt
) {
}
