package com.nucosmos.pos.backend.device;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceAdminResponse(
        UUID id,
        UUID storeId,
        String storeCode,
        String deviceCode,
        String name,
        String platform,
        String status,
        OffsetDateTime lastSeenAt
) {
}
