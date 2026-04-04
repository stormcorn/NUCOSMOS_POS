package com.nucosmos.pos.backend.space;

public record PublicSpaceAvailabilitySlotResponse(
        String startAt,
        String endAt,
        String status,
        String label
) {
}
