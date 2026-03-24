package com.nucosmos.pos.backend.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCustomizationOptionResponse(
        UUID id,
        String name,
        BigDecimal priceDelta,
        boolean defaultSelected,
        int displayOrder,
        boolean active
) {
}
