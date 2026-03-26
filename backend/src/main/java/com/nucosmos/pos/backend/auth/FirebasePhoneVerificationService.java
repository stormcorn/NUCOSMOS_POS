package com.nucosmos.pos.backend.auth;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@ConditionalOnBean(FirebaseApp.class)
public class FirebasePhoneVerificationService implements PhoneVerificationService {

    private final FirebaseAuth firebaseAuth;

    public FirebasePhoneVerificationService(FirebaseApp firebaseApp) {
        this.firebaseAuth = FirebaseAuth.getInstance(firebaseApp);
    }

    @Override
    public PhoneVerificationResult verifyPhoneNumber(
            String expectedPhoneNumber,
            String verificationCode,
            String firebaseIdToken
    ) {
        if (!StringUtils.hasText(firebaseIdToken)) {
            throw new BadRequestException("Firebase ID token is required to complete registration");
        }

        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(firebaseIdToken, true);
            validatePhoneProvider(decodedToken);

            Object phoneNumberClaim = decodedToken.getClaims().get("phone_number");
            String verifiedPhoneNumber = phoneNumberClaim instanceof String value ? value : null;
            if (!StringUtils.hasText(verifiedPhoneNumber)) {
                throw new BadRequestException("Firebase verification did not return a phone number");
            }

            return new PhoneVerificationResult(verifiedPhoneNumber, decodedToken.getUid());
        } catch (FirebaseAuthException ex) {
            throw new BadRequestException("Firebase phone verification failed");
        }
    }

    @SuppressWarnings("unchecked")
    private void validatePhoneProvider(FirebaseToken decodedToken) {
        Object firebaseClaim = decodedToken.getClaims().get("firebase");
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            throw new BadRequestException("Firebase token is missing provider metadata");
        }

        Object signInProvider = firebaseMap.get("sign_in_provider");
        if (!"phone".equals(signInProvider)) {
            throw new BadRequestException("Firebase token was not issued by phone authentication");
        }
    }
}
