package com.nucosmos.pos.backend.auth.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserAdminRequest(
        @NotBlank @Size(max = 50) String employeeCode,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 12) @Pattern(regexp = "^$|\\d+$", message = "PIN must contain digits only") String pin,
        @NotBlank @Size(max = 20) String status,
        @NotEmpty List<String> roleCodes,
        @NotEmpty List<String> storeCodes
) {
}
