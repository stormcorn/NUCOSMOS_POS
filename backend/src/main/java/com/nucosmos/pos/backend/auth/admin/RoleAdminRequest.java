package com.nucosmos.pos.backend.auth.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleAdminRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 255) String description,
        @NotNull Boolean active,
        List<String> permissionKeys
) {
}
