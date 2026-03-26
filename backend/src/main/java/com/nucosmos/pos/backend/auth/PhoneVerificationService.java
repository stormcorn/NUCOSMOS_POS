package com.nucosmos.pos.backend.auth;

public interface PhoneVerificationService {

    PhoneVerificationResult verifyPhoneNumber(
            String expectedPhoneNumber,
            String verificationCode,
            String firebaseIdToken
    );
}
