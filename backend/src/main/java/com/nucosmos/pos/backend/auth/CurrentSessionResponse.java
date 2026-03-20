package com.nucosmos.pos.backend.auth;

import java.util.List;
import java.util.UUID;

public record CurrentSessionResponse(
        UUID userId,
        String employeeCode,
        String displayName,
        String storeCode,
        String activeRole,
        List<String> roleCodes,
        String deviceCode
) {
}
