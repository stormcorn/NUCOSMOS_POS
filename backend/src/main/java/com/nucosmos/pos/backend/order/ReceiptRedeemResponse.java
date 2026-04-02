package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record ReceiptRedeemResponse(
        String token,
        String claimCode,
        String redeemUrl,
        String orderNumber,
        String storeCode,
        String storeName,
        int itemCount,
        BigDecimal totalAmount,
        String paymentStatus,
        OffsetDateTime orderedAt,
        OffsetDateTime claimedAt,
        boolean eligible,
        boolean claimed,
        boolean claimable,
        String message,
        ReceiptMemberSummary member,
        ReceiptRewardSummary rewards,
        ReceiptDrawSummary draw,
        List<ReceiptPrizeSummary> availablePrizes,
        String shareCouponHint
) {
}
