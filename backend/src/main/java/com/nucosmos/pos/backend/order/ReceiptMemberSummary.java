package com.nucosmos.pos.backend.order;

public record ReceiptMemberSummary(
        String displayName,
        String phoneNumber,
        int pointBalance,
        int totalClaims
) {
}
