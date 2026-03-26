package com.nucosmos.pos.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationStartRequest(
        @NotBlank String storeCode,
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,20}$", message = "Phone number format is invalid")
        String phoneNumber,
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "PIN must contain exactly 6 digits")
        String pin
) {
}
