package com.nucosmos.pos.backend.supply;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderResponse(
        UUID id,
        String orderNumber,
        String status,
        String storeCode,
        UUID supplierId,
        String supplierCode,
        String supplierName,
        String createdByEmployeeCode,
        String note,
        OffsetDateTime expectedAt,
        OffsetDateTime receivedAt,
        List<PurchaseOrderLineResponse> lines
) {
}
