package com.nucosmos.pos.backend.space;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminSpaceBlockoutRequest(
        @NotNull UUID spaceResourceId,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 1000) String reason,
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt
) {
}
