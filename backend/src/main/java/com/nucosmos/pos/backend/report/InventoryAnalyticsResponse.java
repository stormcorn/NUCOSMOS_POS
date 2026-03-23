package com.nucosmos.pos.backend.report;

import java.time.OffsetDateTime;
import java.util.List;

public record InventoryAnalyticsResponse(
        String storeCode,
        OffsetDateTime from,
        OffsetDateTime to,
        InventoryKpiSummaryResponse summary,
        List<LowStockSnapshotResponse> lowStockProducts,
        List<LowStockSnapshotResponse> lowStockMaterials,
        List<LowStockSnapshotResponse> lowStockPackaging,
        List<MovementTotalResponse> productMovementTotals,
        List<MovementTotalResponse> materialMovementTotals,
        List<MovementTotalResponse> packagingMovementTotals,
        List<ConsumptionSummaryResponse> materialConsumption,
        List<ConsumptionSummaryResponse> packagingConsumption,
        List<DefectiveWasteSummaryResponse> defectiveAndWaste,
        List<ExpiringLotSnapshotResponse> expiringMaterialLots,
        List<ExpiringLotSnapshotResponse> expiringPackagingLots
) {
}
