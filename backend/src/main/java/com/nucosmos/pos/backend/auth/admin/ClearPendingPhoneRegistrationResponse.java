package com.nucosmos.pos.backend.auth.admin;

public record ClearPendingPhoneRegistrationResponse(
        String phoneNumber,
        int clearedCount,
        String clearedStatus
) {
}
