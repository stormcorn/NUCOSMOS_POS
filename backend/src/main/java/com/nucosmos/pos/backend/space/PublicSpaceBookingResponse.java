package com.nucosmos.pos.backend.space;

import java.math.BigDecimal;

public record PublicSpaceBookingResponse(
        String bookingNumber,
        String status,
        String spaceName,
        String startAt,
        String endAt,
        BigDecimal subtotalAmount,
        BigDecimal balanceAmount,
        String message
) {
}
