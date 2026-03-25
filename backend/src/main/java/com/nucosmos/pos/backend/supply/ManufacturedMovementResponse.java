package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ManufacturedMovementResponse(
        UUID id,
        UUID manufacturedItemId,
        String sku,
        String manufacturedName,
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
