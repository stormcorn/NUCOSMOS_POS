package com.nucosmos.pos.backend.auth;

import com.nucosmos.pos.backend.common.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PinLoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final Map<String, AttemptState> attemptStates = new ConcurrentHashMap<>();

    public void ensureAllowed(String storeCode, String clientIp) {
        AttemptState state = attemptStates.get(buildKey(storeCode, clientIp));
        if (state == null) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (state.blockedUntil != null && state.blockedUntil.isAfter(now)) {
            throw new TooManyRequestsException("Too many PIN login attempts. Please wait 15 minutes and try again.");
        }

        if (state.blockedUntil != null && !state.blockedUntil.isAfter(now)) {
            attemptStates.remove(buildKey(storeCode, clientIp));
        }
    }

    public void recordSuccess(String storeCode, String clientIp) {
        attemptStates.remove(buildKey(storeCode, clientIp));
    }

    public void recordFailure(String storeCode, String clientIp) {
        String key = buildKey(storeCode, clientIp);
        OffsetDateTime now = OffsetDateTime.now();

        attemptStates.compute(key, (ignoredKey, current) -> {
            AttemptState next = current;
            if (next == null || next.lastFailedAt.plus(FAILURE_WINDOW).isBefore(now)) {
                next = new AttemptState(0, now, null);
            }

            int failedAttempts = next.failedAttempts + 1;
            OffsetDateTime blockedUntil = failedAttempts >= MAX_FAILED_ATTEMPTS
                    ? now.plus(LOCKOUT_DURATION)
                    : null;
            return new AttemptState(failedAttempts, now, blockedUntil);
        });
    }

    private String buildKey(String storeCode, String clientIp) {
        String normalizedStoreCode = StringUtils.hasText(storeCode)
                ? storeCode.trim().toUpperCase()
                : "UNKNOWN";
        String normalizedClientIp = StringUtils.hasText(clientIp)
                ? clientIp.trim()
                : "unknown";
        return normalizedStoreCode + "::" + normalizedClientIp;
    }

    private record AttemptState(
            int failedAttempts,
            OffsetDateTime lastFailedAt,
            OffsetDateTime blockedUntil
    ) {
    }
}
