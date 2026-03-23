package com.nucosmos.pos.backend.inventory.stocktake;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InventoryStocktakeResponse(
        UUID id,
        String status,
        String storeCode,
        String createdByEmployeeCode,
        String note,
        OffsetDateTime countedAt,
        OffsetDateTime postedAt,
        List<InventoryStocktakeItemResponse> items
) {
}
