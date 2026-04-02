package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicMemberLoginRequest(
        @Size(max = 80)
        String displayName,
        @NotBlank
        String firebaseIdToken
) {
}
