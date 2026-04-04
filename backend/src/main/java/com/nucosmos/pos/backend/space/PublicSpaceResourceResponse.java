package com.nucosmos.pos.backend.space;

import java.math.BigDecimal;

public record PublicSpaceResourceResponse(
        String code,
        String name,
        String slug,
        String description,
        String locationLabel,
        int capacity,
        String timezone,
        BigDecimal hourlyRate,
        String currencyCode,
        int minimumHours,
        int bookingIntervalMinutes,
        int bufferBeforeMinutes,
        int bufferAfterMinutes,
        int maxAttendees,
        String cancellationPolicyText,
        String houseRulesText
) {
}
