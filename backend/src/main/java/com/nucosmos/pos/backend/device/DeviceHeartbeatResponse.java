package com.nucosmos.pos.backend.device;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceHeartbeatResponse(
        UUID deviceId,
        String storeCode,
        String deviceCode,
        String status,
        OffsetDateTime lastSeenAt,
        OffsetDateTime serverTime
) {
}
