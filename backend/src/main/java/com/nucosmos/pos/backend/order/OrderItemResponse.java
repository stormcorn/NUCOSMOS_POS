package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        int lineNumber,
        UUID productId,
        String productSku,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotalAmount,
        BigDecimal unitCostAmount,
        BigDecimal lineCostAmount,
        BigDecimal refundedCostAmount,
        String note
) {
}
