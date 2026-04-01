package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicRedeemClaimRequest(
        @NotBlank
        @Size(max = 80)
        String displayName,
        @NotBlank
        @Size(max = 30)
        String phoneNumber
) {
}
