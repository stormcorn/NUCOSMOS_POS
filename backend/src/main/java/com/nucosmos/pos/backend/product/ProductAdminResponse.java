package com.nucosmos.pos.backend.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductAdminResponse(
        UUID id,
        UUID categoryId,
        String categoryCode,
        String categoryName,
        String sku,
        String name,
        String description,
        BigDecimal price,
        boolean active
) {
}
