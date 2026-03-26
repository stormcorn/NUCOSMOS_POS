package com.nucosmos.pos.backend.auth.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClearPendingPhoneRegistrationRequest(
        @NotBlank
        @Size(max = 30)
        @Pattern(regexp = "^\\+?[0-9\\-\\s]{10,20}$", message = "Phone number format is invalid")
        String phoneNumber
) {
}
