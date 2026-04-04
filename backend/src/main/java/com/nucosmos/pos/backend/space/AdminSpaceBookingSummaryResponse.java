package com.nucosmos.pos.backend.space;

import java.math.BigDecimal;

public record AdminSpaceBookingSummaryResponse(
        String id,
        String bookingNumber,
        String status,
        String source,
        String spaceName,
        String customerName,
        String customerPhone,
        String purpose,
        int attendeeCount,
        String startAt,
        String endAt,
        BigDecimal subtotalAmount,
        String approvedAt
) {
}
