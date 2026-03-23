package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        String refundMethod,
        BigDecimal amount,
        String reason,
        String status,
        String createdByEmployeeCode,
        OffsetDateTime refundedAt,
        List<RefundItemResponse> items
) {
}
