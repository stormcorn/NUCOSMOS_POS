package com.nucosmos.pos.backend.auth;

import java.util.List;
import java.util.UUID;

public record AuthStaffResponse(
        UUID id,
        String employeeCode,
        String displayName,
        List<String> roleCodes,
        String activeRole,
        List<String> permissionKeys
) {
}
