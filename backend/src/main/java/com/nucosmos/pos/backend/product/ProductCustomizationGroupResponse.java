package com.nucosmos.pos.backend.product;

import java.util.List;
import java.util.UUID;

public record ProductCustomizationGroupResponse(
        UUID id,
        String name,
        String selectionMode,
        boolean required,
        int minSelections,
        int maxSelections,
        int displayOrder,
        boolean active,
        List<ProductCustomizationOptionResponse> options
) {
}
