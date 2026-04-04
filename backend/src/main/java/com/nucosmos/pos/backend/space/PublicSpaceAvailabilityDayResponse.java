package com.nucosmos.pos.backend.space;

import java.util.List;

public record PublicSpaceAvailabilityDayResponse(
        String date,
        List<PublicSpaceAvailabilitySlotResponse> slots
) {
}
