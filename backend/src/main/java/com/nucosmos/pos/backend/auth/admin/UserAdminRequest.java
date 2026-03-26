package com.nucosmos.pos.backend.auth.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UserAdminRequest(
        @NotBlank @Size(max = 50) String employeeCode,
        @NotBlank @Size(max = 120) String displayName,
        @Pattern(regexp = "^$|\\d{6}$", message = "PIN must contain exactly 6 digits") String pin,
        @NotBlank @Size(max = 20) String status,
        @NotEmpty List<String> roleCodes,
        @NotEmpty List<String> storeCodes
) {
}
