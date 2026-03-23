package com.nucosmos.pos.backend.product;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String sku,
        String name,
        String description,
        String imageUrl,
        String categoryCode,
        String categoryName,
        BigDecimal originalPrice,
        BigDecimal price,
        boolean campaignEnabled,
        boolean campaignActive,
        String campaignLabel,
        BigDecimal campaignPrice,
        OffsetDateTime campaignStartsAt,
        OffsetDateTime campaignEndsAt,
        boolean available
) {
}
