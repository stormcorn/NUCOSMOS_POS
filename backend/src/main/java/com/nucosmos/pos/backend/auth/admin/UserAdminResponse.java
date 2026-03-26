package com.nucosmos.pos.backend.auth.admin;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record UserAdminResponse(
        UUID id,
        String employeeCode,
        String displayName,
        String phoneNumber,
        String status,
        OffsetDateTime lastLoginAt,
        List<String> roleCodes,
        List<String> storeCodes,
        List<String> storeNames
) {
}
