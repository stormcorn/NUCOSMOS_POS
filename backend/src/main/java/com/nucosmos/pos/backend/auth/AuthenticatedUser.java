package com.nucosmos.pos.backend.auth;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID userId,
        String employeeCode,
        String displayName,
        String storeCode,
        String activeRole,
        List<String> roleCodes,
        List<String> permissionKeys,
        String deviceCode
) {
}
