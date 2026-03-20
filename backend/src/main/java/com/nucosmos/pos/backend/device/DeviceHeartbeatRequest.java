package com.nucosmos.pos.backend.device;

import jakarta.validation.constraints.Size;

public record DeviceHeartbeatRequest(
        @Size(max = 50) String deviceCode
) {
}
