package com.nucosmos.pos.backend.auth;

public record PhoneVerificationResult(
        String phoneNumber,
        String firebaseUid
) {
}
