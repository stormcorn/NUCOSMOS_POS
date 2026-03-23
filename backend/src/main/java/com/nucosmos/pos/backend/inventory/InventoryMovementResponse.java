package com.nucosmos.pos.backend.inventory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryMovementResponse(
        UUID id,
        UUID productId,
        String sku,
        String productName,
        String movementType,
        String stockBucket,
        int quantity,
        int quantityDelta,
        int quantityAfter,
        int sellableQuantityDelta,
        int defectiveQuantityDelta,
        int sellableQuantityAfter,
        int defectiveQuantityAfter,
        BigDecimal unitCost,
        String reasonCode,
        String note,
        String referenceType,
        UUID referenceId,
        OffsetDateTime occurredAt
) {
}
