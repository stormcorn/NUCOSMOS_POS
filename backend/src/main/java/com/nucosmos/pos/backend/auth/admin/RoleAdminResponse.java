package com.nucosmos.pos.backend.auth.admin;

import java.util.List;
import java.util.UUID;

public record RoleAdminResponse(
        UUID id,
        String code,
        String name,
        String description,
        boolean active,
        List<String> permissionKeys
) {
}
