package com.nucosmos.pos.backend.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductRecipeVersionSummaryResponse(
        UUID id,
        int versionNumber,
        String status,
        String note,
        OffsetDateTime effectiveAt,
        int materialComponentCount,
        int manufacturedComponentCount,
        int packagingComponentCount,
        BigDecimal materialCost,
        BigDecimal manufacturedCost,
        BigDecimal packagingCost,
        BigDecimal totalCost
) {
}
