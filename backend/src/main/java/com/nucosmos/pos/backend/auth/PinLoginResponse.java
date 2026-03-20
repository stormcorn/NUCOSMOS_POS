package com.nucosmos.pos.backend.auth;

import java.time.OffsetDateTime;

public record PinLoginResponse(
        String tokenType,
        String accessToken,
        OffsetDateTime expiresAt,
        String deviceCode,
        AuthStoreResponse store,
        AuthStaffResponse staff
) {
}
