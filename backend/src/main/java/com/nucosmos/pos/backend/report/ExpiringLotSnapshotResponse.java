package com.nucosmos.pos.backend.report;

import java.time.OffsetDateTime;

public record ExpiringLotSnapshotResponse(
        String scope,
        String sku,
        String name,
        String batchCode,
        OffsetDateTime expiryDate,
        int remainingQuantity,
        String unit,
        long daysUntilExpiry
) {
}
