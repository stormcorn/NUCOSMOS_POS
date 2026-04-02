package com.nucosmos.pos.backend.order;

public record ReceiptRewardSummary(
        int awardedPoints,
        int pointsBalance,
        ReceiptCouponSummary issuedCoupon,
        String message,
        int nextCouponThreshold,
        java.math.BigDecimal nextCouponAmount
) {
}
