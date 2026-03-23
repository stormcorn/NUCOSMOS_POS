package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MaterialMovementResponse(
        UUID id,
        UUID materialId,
        String sku,
        String materialName,
        String unit,
        String movementType,
        int quantity,
        int quantityDelta,
        int quantityAfter,
        BigDecimal unitCost,
        String note,
        OffsetDateTime occurredAt
) {
}
