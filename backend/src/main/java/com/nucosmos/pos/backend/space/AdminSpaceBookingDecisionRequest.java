package com.nucosmos.pos.backend.space;

import jakarta.validation.constraints.Size;

public record AdminSpaceBookingDecisionRequest(
        @Size(max = 1000) String internalNote
) {
}
