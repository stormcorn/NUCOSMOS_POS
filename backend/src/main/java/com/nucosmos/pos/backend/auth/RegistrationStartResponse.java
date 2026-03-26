package com.nucosmos.pos.backend.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistrationStartResponse(
        UUID registrationId,
        String storeCode,
        String phoneNumber,
        String provider,
        String status,
        OffsetDateTime expiresAt
) {
}
