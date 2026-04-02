package com.nucosmos.pos.backend.order;

public record ReceiptDrawSummary(
        String outcome,
        boolean won,
        String title,
        String message,
        ReceiptPrizeSummary prize
) {
}
