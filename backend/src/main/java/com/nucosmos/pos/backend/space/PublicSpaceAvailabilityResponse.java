package com.nucosmos.pos.backend.space;

import java.util.List;

public record PublicSpaceAvailabilityResponse(
        String spaceSlug,
        String spaceName,
        String timezone,
        int bookingIntervalMinutes,
        List<PublicSpaceAvailabilityDayResponse> days
) {
}
