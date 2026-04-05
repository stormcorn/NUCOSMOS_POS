package com.nucosmos.pos.backend.space;

import java.math.BigDecimal;

public record AdminSpaceBookingResponse(
        String id,
        String bookingNumber,
        String status,
        String source,
        String spaceId,
        String spaceName,
        String customerName,
        String customerPhone,
        String customerEmail,
        String purpose,
        String eventLink,
        int attendeeCount,
        BigDecimal subtotalAmount,
        BigDecimal depositAmount,
        BigDecimal paidAmount,
        BigDecimal balanceAmount,
        String note,
        String internalNote,
        String startAt,
        String endAt,
        String approvedAt,
        String approvedBy,
        String cancelledAt
) {
}
