package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        boolean testOrder,
        String status,
        String paymentStatus,
        String storeCode,
        String deviceCode,
        String createdByEmployeeCode,
        int itemCount,
        BigDecimal subtotalAmount,
        String discountType,
        BigDecimal discountValue,
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
        String redeemCode,
        String redeemUrl,
        List<OrderItemResponse> items,
        List<PaymentResponse> payments,
        List<RefundResponse> refunds
) {
}
