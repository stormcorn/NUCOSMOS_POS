package com.nucosmos.pos.backend.space;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record PublicSpaceMemberBookingUpdateRequest(
        @Size(max = 240) String purpose,
        @Size(max = 500) String eventLink,
        @Min(1) @Max(500) int attendeeCount,
        @Size(max = 1000) String note,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt
) {
}
