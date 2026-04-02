package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.util.UUID;

public record ReceiptPrizeSummary(
        UUID id,
        String name,
        String description,
        BigDecimal probabilityPercent,
        int remainingQuantity,
        boolean active,
        int displayOrder
) {
}
