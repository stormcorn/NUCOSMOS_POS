package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record BulkDeleteTestOrdersRequest(
        @NotNull OffsetDateTime from,
        @NotNull OffsetDateTime to
) {
}
