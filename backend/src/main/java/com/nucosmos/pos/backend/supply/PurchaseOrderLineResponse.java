package com.nucosmos.pos.backend.supply;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PurchaseOrderLineResponse(
        UUID id,
        String itemType,
        UUID itemId,
        String itemSku,
        String itemName,
        String unit,
        String stockUnit,
        int purchaseToStockRatio,
        int orderedQuantity,
        int receivedQuantity,
        int receivedStockQuantity,
        BigDecimal unitCost,
        String batchCode,
        OffsetDateTime expiryDate,
        OffsetDateTime manufacturedAt,
        String note
) {
}
