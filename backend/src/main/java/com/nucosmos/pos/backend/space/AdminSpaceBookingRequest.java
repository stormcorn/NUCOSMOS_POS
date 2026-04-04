package com.nucosmos.pos.backend.space;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminSpaceBookingRequest(
        @NotNull UUID spaceResourceId,
        @NotBlank @Size(max = 120) String customerName,
        @NotBlank @Size(max = 40) String customerPhone,
        @Email @Size(max = 160) String customerEmail,
        @Size(max = 240) String purpose,
        @Min(1) @Max(500) int attendeeCount,
        @Size(max = 1000) String note,
        @Size(max = 1000) String internalNote,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        @Size(max = 30) String source,
        @Size(max = 30) String status
) {
}
