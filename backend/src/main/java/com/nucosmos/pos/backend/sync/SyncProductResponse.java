package com.nucosmos.pos.backend.sync;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SyncProductResponse(
        UUID id,
        UUID categoryId,
        String categoryCode,
        String sku,
        String name,
        String description,
        BigDecimal price,
        boolean active,
        OffsetDateTime updatedAt
) {
}
