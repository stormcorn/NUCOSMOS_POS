package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.FirebaseWebProperties;
import com.nucosmos.pos.backend.auth.PhoneVerificationResult;
import com.nucosmos.pos.backend.auth.PhoneVerificationService;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberSessionEntity;
import com.nucosmos.pos.backend.order.repository.ReceiptMemberRepository;
import com.nucosmos.pos.backend.order.repository.ReceiptMemberSessionRepository;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class PublicMemberAuthService {

    public static final String SESSION_COOKIE_NAME = "nucosmos_member_session";

    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final Duration SESSION_TTL = Duration.ofDays(30);

    private final FirebaseWebProperties firebaseWebProperties;
    private final PhoneVerificationService phoneVerificationService;
    private final ReceiptMemberRepository receiptMemberRepository;
    private final ReceiptMemberSessionRepository receiptMemberSessionRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PublicMemberAuthService(
            FirebaseWebProperties firebaseWebProperties,
            PhoneVerificationService phoneVerificationService,
            ReceiptMemberRepository receiptMemberRepository,
            ReceiptMemberSessionRepository receiptMemberSessionRepository
    ) {
        this.firebaseWebProperties = firebaseWebProperties;
        this.phoneVerificationService = phoneVerificationService;
        this.receiptMemberRepository = receiptMemberRepository;
        this.receiptMemberSessionRepository = receiptMemberSessionRepository;
    }

    @Transactional(readOnly = true)
    public PublicMemberFirebaseConfigResponse firebaseConfig() {
        return new PublicMemberFirebaseConfigResponse(
                firebaseWebProperties.isConfigured(),
                firebaseWebProperties.getApiKey(),
                firebaseWebProperties.getAuthDomain(),
                firebaseWebProperties.getProjectId(),
                firebaseWebProperties.getStorageBucket(),
                firebaseWebProperties.getMessagingSenderId(),
                firebaseWebProperties.getAppId(),
                firebaseWebProperties.getMeasurementId()
        );
    }

    @Transactional
    public LoginResult loginWithSms(PublicMemberLoginRequest request) {
        PhoneVerificationResult verification = phoneVerificationService.verifyPhoneNumber(
                "",
                "",
                request.firebaseIdToken().trim()
        );

        String phoneNumber = normalizePhoneNumber(verification.phoneNumber());
        String displayName = normalizeOptionalDisplayName(request.displayName());
        OffsetDateTime now = OffsetDateTime.now();

        ReceiptMemberEntity member = receiptMemberRepository.findByPhoneNumber(phoneNumber)
                .map(existing -> {
                    if (StringUtils.hasText(displayName)) {
                        existing.updateProfile(displayName, phoneNumber);
                    }
                    existing.bindFirebaseUid(verification.firebaseUid());
                    return existing;
                })
                .orElseGet(() -> {
                    if (!StringUtils.hasText(displayName)) {
                        throw new BadRequestException("Display name is required for first-time member login");
                    }
                    ReceiptMemberEntity created = new ReceiptMemberEntity(displayName, phoneNumber, ACTIVE_STATUS);
                    created.bindFirebaseUid(verification.firebaseUid());
                    return receiptMemberRepository.save(created);
                });

        ReceiptMemberSessionEntity session = new ReceiptMemberSessionEntity(
                member,
                generateUniqueSessionToken(),
                now.plus(SESSION_TTL),
                now
        );
        receiptMemberSessionRepository.save(session);

        return new LoginResult(toSessionResponse(member), buildSessionCookie(session.getPublicToken()));
    }

    @Transactional(readOnly = true)
    public PublicMemberSessionResponse currentSession(String sessionToken) {
        return findActiveMember(sessionToken)
                .map(member -> new PublicMemberSessionResponse(true, toMemberSummary(member)))
                .orElseGet(() -> new PublicMemberSessionResponse(false, null));
    }

    @Transactional
    public void logout(String sessionToken) {
        if (!StringUtils.hasText(sessionToken)) {
            return;
        }
        receiptMemberSessionRepository.findByPublicToken(sessionToken.trim())
                .ifPresent(receiptMemberSessionRepository::delete);
    }

    @Transactional
    public Optional<ReceiptMemberEntity> findActiveMember(String sessionToken) {
        if (!StringUtils.hasText(sessionToken)) {
            return Optional.empty();
        }

        OffsetDateTime now = OffsetDateTime.now();
        return receiptMemberSessionRepository.findByPublicToken(sessionToken.trim())
                .flatMap(session -> {
                    if (session.isExpired(now)) {
                        receiptMemberSessionRepository.delete(session);
                        return Optional.empty();
                    }
                    session.refresh(now, now.plus(SESSION_TTL));
                    return Optional.of(session.getMember());
                });
    }

    public ResponseCookie clearSessionCookie() {
        return ResponseCookie.from(SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ReceiptMemberSummary toMemberSummary(ReceiptMemberEntity member) {
        return new ReceiptMemberSummary(
                member.getDisplayName(),
                member.getPhoneNumber(),
                member.getPointBalance(),
                member.getTotalClaims()
        );
    }

    private PublicMemberSessionResponse toSessionResponse(ReceiptMemberEntity member) {
        return new PublicMemberSessionResponse(true, toMemberSummary(member));
    }

    private ResponseCookie buildSessionCookie(String sessionToken) {
        return ResponseCookie.from(SESSION_COOKIE_NAME, sessionToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(SESSION_TTL)
                .build();
    }

    private String generateUniqueSessionToken() {
        String token;
        do {
            byte[] bytes = new byte[32];
            secureRandom.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } while (receiptMemberSessionRepository.existsByPublicToken(token));
        return token;
    }

    private String normalizePhoneNumber(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.replaceAll("[\\s\\-()]", "").trim();
        if (normalized.matches("^09\\d{8}$")) {
            return "+886" + normalized.substring(1);
        }
        if (normalized.matches("^\\d{10,15}$")) {
            return "+" + normalized;
        }
        if (!normalized.matches("^\\+\\d{10,15}$")) {
            throw new BadRequestException("Phone number format is invalid");
        }
        return normalized;
    }

    private String normalizeOptionalDisplayName(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }

    public record LoginResult(
            PublicMemberSessionResponse session,
            ResponseCookie sessionCookie
    ) {
    }
}
