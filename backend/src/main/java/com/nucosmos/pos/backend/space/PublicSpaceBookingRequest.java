package com.nucosmos.pos.backend.space;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record PublicSpaceBookingRequest(
        @NotBlank @Size(max = 120) String customerName,
        @NotBlank @Size(max = 40) String customerPhone,
        @Email @Size(max = 160) String customerEmail,
        @Size(max = 240) String purpose,
        @Min(1) @Max(500) int attendeeCount,
        @Size(max = 1000) String note,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt
) {
}
