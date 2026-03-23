package com.nucosmos.pos.backend.report;

public record LowStockSnapshotResponse(
        String itemType,
        String sku,
        String name,
        String secondaryLabel,
        String unit,
        int quantityOnHand,
        int reorderLevel
) {
}
