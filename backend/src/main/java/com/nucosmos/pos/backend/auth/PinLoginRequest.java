package com.nucosmos.pos.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PinLoginRequest(
        @NotBlank String storeCode,
        @Size(max = 50) String roleCode,
        @NotBlank
        @Pattern(regexp = "\\d{4,6}", message = "PIN must contain 4 to 6 digits")
        String pin,
        @Size(max = 50) String deviceCode,
        @Size(max = 120) String deviceName,
        @Size(max = 30) String devicePlatform
) {
}
