package com.nucosmos.pos.backend.auth;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RegistrationCompleteResponse(
        UUID userId,
        String employeeCode,
        String displayName,
        String storeCode,
        String phoneNumber,
        String status,
        OffsetDateTime activatedAt
) {
}
