package com.nucosmos.pos.backend.sync;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SyncCategoryResponse(
        UUID id,
        String code,
        String name,
        Integer displayOrder,
        boolean active,
        OffsetDateTime updatedAt
) {
}
