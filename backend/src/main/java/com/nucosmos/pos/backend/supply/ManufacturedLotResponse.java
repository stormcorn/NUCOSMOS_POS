package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ManufacturedLotResponse(
        UUID id,
        UUID manufacturedItemId,
        String sku,
        String manufacturedName,
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
