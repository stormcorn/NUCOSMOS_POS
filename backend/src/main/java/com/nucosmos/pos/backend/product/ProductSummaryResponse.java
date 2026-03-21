package com.nucosmos.pos.backend.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String sku,
        String name,
        String description,
        String imageUrl,
        String categoryCode,
        String categoryName,
        BigDecimal price,
        boolean available
) {
}
