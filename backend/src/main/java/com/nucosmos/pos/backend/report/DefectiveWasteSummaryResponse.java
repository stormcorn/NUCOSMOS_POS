package com.nucosmos.pos.backend.report;

public record DefectiveWasteSummaryResponse(
        String sku,
        String name,
        String movementType,
        int affectedQuantity
) {
}
