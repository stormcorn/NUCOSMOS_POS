package com.nucosmos.pos.backend.report;

public record InventoryKpiSummaryResponse(
        int productSkuCount,
        int productLowStockCount,
        int totalSellableQuantity,
        int totalDefectiveQuantity,
        int materialSkuCount,
        int materialLowStockCount,
        int totalMaterialQuantity,
        int packagingSkuCount,
        int packagingLowStockCount,
        int totalPackagingQuantity
) {
}
