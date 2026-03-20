package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        String status,
        String paymentStatus,
        String storeCode,
        String deviceCode,
        String createdByEmployeeCode,
        int itemCount,
        BigDecimal subtotalAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal changeAmount,
        BigDecimal refundedAmount,
        String note,
        OffsetDateTime orderedAt,
        OffsetDateTime closedAt,
        OffsetDateTime voidedAt,
        String voidNote,
        List<OrderItemResponse> items,
        List<PaymentResponse> payments,
        List<RefundResponse> refunds
) {
}
