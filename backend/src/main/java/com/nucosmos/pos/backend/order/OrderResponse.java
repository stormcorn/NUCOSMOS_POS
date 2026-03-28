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
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal changeAmount,
        BigDecimal refundedAmount,
        BigDecimal cogsAmount,
        BigDecimal refundedCogsAmount,
        BigDecimal netCogsAmount,
        BigDecimal grossProfitAmount,
        String note,
        String discountNote,
        OffsetDateTime orderedAt,
        OffsetDateTime closedAt,
        OffsetDateTime voidedAt,
        String voidNote,
        List<OrderItemResponse> items,
        List<PaymentResponse> payments,
        List<RefundResponse> refunds
) {
}
