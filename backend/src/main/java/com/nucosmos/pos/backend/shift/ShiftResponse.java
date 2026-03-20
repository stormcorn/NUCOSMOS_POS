package com.nucosmos.pos.backend.shift;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShiftResponse(
        UUID id,
        String status,
        String storeCode,
        String deviceCode,
        String openedByEmployeeCode,
        String closedByEmployeeCode,
        BigDecimal openingCashAmount,
        BigDecimal closingCashAmount,
        BigDecimal expectedCashAmount,
        BigDecimal cashSalesAmount,
        BigDecimal cardSalesAmount,
        BigDecimal refundedAmount,
        BigDecimal netSalesAmount,
        int orderCount,
        int voidedOrderCount,
        String note,
        String closeNote,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt
) {
}
