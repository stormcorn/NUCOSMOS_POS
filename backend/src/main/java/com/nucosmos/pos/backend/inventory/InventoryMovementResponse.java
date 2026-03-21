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
        int quantity,
        int quantityDelta,
        int quantityAfter,
        BigDecimal unitCost,
        String note,
        String referenceType,
        UUID referenceId,
        OffsetDateTime occurredAt
) {
}
