package com.nucosmos.pos.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegistrationCompleteRequest(
        @NotNull UUID registrationId,
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "Verification code must contain exactly 6 digits")
        String verificationCode,
        @Size(max = 4096) String firebaseIdToken
) {
}
