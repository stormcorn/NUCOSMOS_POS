package com.nucosmos.pos.backend.order;

import com.nucosmos.pos.backend.auth.FirebaseWebProperties;
import com.nucosmos.pos.backend.auth.PhoneVerificationResult;
import com.nucosmos.pos.backend.auth.PhoneVerificationService;
import com.nucosmos.pos.backend.common.exception.BadRequestException;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberSessionEntity;
import com.nucosmos.pos.backend.order.persistence.ReceiptMemberTrustedDeviceEntity;
import com.nucosmos.pos.backend.order.repository.ReceiptMemberRepository;
import com.nucosmos.pos.backend.order.repository.ReceiptMemberSessionRepository;
import com.nucosmos.pos.backend.order.repository.ReceiptMemberTrustedDeviceRepository;
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
    private static final Duration SESSION_TTL = Duration.ofDays(90);
    private static final Duration TRUSTED_DEVICE_TTL = Duration.ofDays(365);

    private final FirebaseWebProperties firebaseWebProperties;
    private final PhoneVerificationService phoneVerificationService;
    private final ReceiptMemberRepository receiptMemberRepository;
    private final ReceiptMemberSessionRepository receiptMemberSessionRepository;
    private final ReceiptMemberTrustedDeviceRepository receiptMemberTrustedDeviceRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public PublicMemberAuthService(
            FirebaseWebProperties firebaseWebProperties,
            PhoneVerificationService phoneVerificationService,
            ReceiptMemberRepository receiptMemberRepository,
            ReceiptMemberSessionRepository receiptMemberSessionRepository,
            ReceiptMemberTrustedDeviceRepository receiptMemberTrustedDeviceRepository
    ) {
        this.firebaseWebProperties = firebaseWebProperties;
        this.phoneVerificationService = phoneVerificationService;
        this.receiptMemberRepository = receiptMemberRepository;
        this.receiptMemberSessionRepository = receiptMemberSessionRepository;
        this.receiptMemberTrustedDeviceRepository = receiptMemberTrustedDeviceRepository;
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
    public LoginResult loginWithSms(PublicMemberLoginRequest request, String deviceToken, String deviceLabel) {
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

        boolean deviceTrusted = false;
        String normalizedDeviceToken = normalizeOptionalToken(deviceToken);
        if (request.rememberDevice() && StringUtils.hasText(normalizedDeviceToken)) {
            rememberTrustedDevice(member, normalizedDeviceToken, deviceLabel, now);
            deviceTrusted = true;
        } else if (StringUtils.hasText(normalizedDeviceToken)) {
            receiptMemberTrustedDeviceRepository.deleteByDeviceToken(normalizedDeviceToken);
        }

        ReceiptMemberSessionEntity session = new ReceiptMemberSessionEntity(
                member,
                generateUniqueSessionToken(),
                now.plus(SESSION_TTL),
                now
        );
        receiptMemberSessionRepository.save(session);

        return new LoginResult(toSessionResponse(member, deviceTrusted), buildSessionCookie(session.getPublicToken()));
    }

    @Transactional
    public SessionResult currentSession(String sessionToken, String deviceToken, String deviceLabel) {
        Optional<AuthenticatedMemberResult> authenticated = resolveAuthenticatedMember(sessionToken, deviceToken, deviceLabel);
        return authenticated
                .map(result -> new SessionResult(toSessionResponse(result.member(), result.deviceTrusted()), result.sessionCookie()))
                .orElseGet(() -> new SessionResult(new PublicMemberSessionResponse(false, null, false), null));
    }

    @Transactional
    public void logout(String sessionToken, String deviceToken) {
        if (StringUtils.hasText(sessionToken)) {
            receiptMemberSessionRepository.findByPublicToken(sessionToken.trim())
                    .ifPresent(receiptMemberSessionRepository::delete);
        }
        if (StringUtils.hasText(deviceToken)) {
            receiptMemberTrustedDeviceRepository.deleteByDeviceToken(normalizeOptionalToken(deviceToken));
        }
    }

    @Transactional
    public Optional<AuthenticatedMemberResult> resolveAuthenticatedMember(
            String sessionToken,
            String deviceToken,
            String deviceLabel
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        Optional<ReceiptMemberSessionEntity> session = findActiveSession(sessionToken, now);
        if (session.isPresent()) {
            ReceiptMemberEntity member = session.get().getMember();
            boolean deviceTrusted = refreshTrustedDeviceIfOwned(member, deviceToken, deviceLabel, now);
            return Optional.of(new AuthenticatedMemberResult(member, deviceTrusted, null));
        }

        String normalizedDeviceToken = normalizeOptionalToken(deviceToken);
        if (!StringUtils.hasText(normalizedDeviceToken)) {
            return Optional.empty();
        }

        return receiptMemberTrustedDeviceRepository.findByDeviceToken(normalizedDeviceToken)
                .flatMap(device -> {
                    if (device.isExpired(now)) {
                        receiptMemberTrustedDeviceRepository.delete(device);
                        return Optional.empty();
                    }
                    device.refresh(now, now.plus(TRUSTED_DEVICE_TTL));
                    ReceiptMemberEntity member = device.getMember();
                    ReceiptMemberSessionEntity restoredSession = new ReceiptMemberSessionEntity(
                            member,
                            generateUniqueSessionToken(),
                            now.plus(SESSION_TTL),
                            now
                    );
                    receiptMemberSessionRepository.save(restoredSession);
                    return Optional.of(new AuthenticatedMemberResult(member, true, buildSessionCookie(restoredSession.getPublicToken())));
                });
    }

    private Optional<ReceiptMemberSessionEntity> findActiveSession(String sessionToken, OffsetDateTime now) {
        if (!StringUtils.hasText(sessionToken)) {
            return Optional.empty();
        }

        return receiptMemberSessionRepository.findByPublicToken(sessionToken.trim())
                .flatMap(session -> {
                    if (session.isExpired(now)) {
                        receiptMemberSessionRepository.delete(session);
                        return Optional.empty();
                    }
                    session.refresh(now, now.plus(SESSION_TTL));
                    return Optional.of(session);
                });
    }

    private boolean refreshTrustedDeviceIfOwned(
            ReceiptMemberEntity member,
            String deviceToken,
            String deviceLabel,
            OffsetDateTime now
    ) {
        String normalizedDeviceToken = normalizeOptionalToken(deviceToken);
        if (!StringUtils.hasText(normalizedDeviceToken)) {
            return false;
        }

        return receiptMemberTrustedDeviceRepository.findByDeviceToken(normalizedDeviceToken)
                .map(device -> {
                    if (device.isExpired(now)) {
                        receiptMemberTrustedDeviceRepository.delete(device);
                        return false;
                    }
                    if (!device.getMember().getId().equals(member.getId())) {
                        return false;
                    }
                    device.refresh(now, now.plus(TRUSTED_DEVICE_TTL));
                    if (StringUtils.hasText(deviceLabel) && !deviceLabel.trim().equals(device.getDeviceLabel())) {
                        device.relink(member, normalizeDeviceLabel(deviceLabel), now, now.plus(TRUSTED_DEVICE_TTL));
                    }
                    return true;
                })
                .orElse(false);
    }

    private void rememberTrustedDevice(
            ReceiptMemberEntity member,
            String deviceToken,
            String deviceLabel,
            OffsetDateTime now
    ) {
        String normalizedDeviceToken = normalizeOptionalToken(deviceToken);
        if (!StringUtils.hasText(normalizedDeviceToken)) {
            return;
        }

        String normalizedDeviceLabel = normalizeDeviceLabel(deviceLabel);
        receiptMemberTrustedDeviceRepository.findByDeviceToken(normalizedDeviceToken)
                .ifPresentOrElse(existing -> {
                    existing.relink(member, normalizedDeviceLabel, now, now.plus(TRUSTED_DEVICE_TTL));
                }, () -> receiptMemberTrustedDeviceRepository.save(
                        new ReceiptMemberTrustedDeviceEntity(
                                member,
                                normalizedDeviceToken,
                                normalizedDeviceLabel,
                                now.plus(TRUSTED_DEVICE_TTL),
                                now
                        )
                ));
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

    private PublicMemberSessionResponse toSessionResponse(ReceiptMemberEntity member, boolean deviceTrusted) {
        return new PublicMemberSessionResponse(true, toMemberSummary(member), deviceTrusted);
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

    private String normalizeOptionalToken(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }

    private String normalizeDeviceLabel(String rawValue) {
        String normalized = rawValue == null ? "" : rawValue.trim();
        if (!StringUtils.hasText(normalized)) {
            return "Redeem Browser";
        }
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    public record LoginResult(
            PublicMemberSessionResponse session,
            ResponseCookie sessionCookie
    ) {
    }

    public record SessionResult(
            PublicMemberSessionResponse session,
            ResponseCookie sessionCookie
    ) {
    }

    public record AuthenticatedMemberResult(
            ReceiptMemberEntity member,
            boolean deviceTrusted,
            ResponseCookie sessionCookie
    ) {
    }
}
