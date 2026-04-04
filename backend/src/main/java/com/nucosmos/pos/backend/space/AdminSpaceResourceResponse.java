package com.nucosmos.pos.backend.space;

import java.math.BigDecimal;

public record AdminSpaceResourceResponse(
        String id,
        String code,
        String name,
        String slug,
        String locationLabel,
        int capacity,
        boolean active,
        String timezone,
        BigDecimal hourlyRate,
        String currencyCode,
        int minimumHours,
        int bookingIntervalMinutes,
        int bufferBeforeMinutes,
        int bufferAfterMinutes,
        int maxAttendees
) {
}
