package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PackagingLotResponse(
        UUID id,
        UUID packagingItemId,
        String sku,
        String packagingName,
        String unit,
        String batchCode,
        OffsetDateTime expiryDate,
        OffsetDateTime manufacturedAt,
        int receivedQuantity,
        int remainingQuantity,
        BigDecimal unitCost,
        String sourceType,
        UUID sourceId,
        OffsetDateTime receivedAt
) {
}
