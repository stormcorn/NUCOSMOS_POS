package com.nucosmos.pos.backend.product;

import java.util.UUID;

public record ProductCategorySummaryResponse(
        UUID id,
        String code,
        String name,
        Integer displayOrder,
        boolean active
) {
}
