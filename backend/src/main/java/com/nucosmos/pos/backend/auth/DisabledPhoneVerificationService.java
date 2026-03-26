package com.nucosmos.pos.backend.auth;

import com.nucosmos.pos.backend.common.exception.BadRequestException;

public class DisabledPhoneVerificationService implements PhoneVerificationService {

    @Override
    public PhoneVerificationResult verifyPhoneNumber(
            String expectedPhoneNumber,
            String verificationCode,
            String firebaseIdToken
    ) {
        throw new BadRequestException("Firebase phone verification is not configured yet.");
    }
}
