package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ReceiptCouponSummary(
        String code,
        String title,
        BigDecimal discountAmount,
        String status,
        OffsetDateTime issuedAt
) {
}
