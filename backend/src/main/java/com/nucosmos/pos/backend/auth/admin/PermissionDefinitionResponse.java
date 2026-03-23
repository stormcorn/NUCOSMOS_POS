package com.nucosmos.pos.backend.auth.admin;

public record PermissionDefinitionResponse(
        String key,
        String label,
        String groupName,
        String description
) {
}
