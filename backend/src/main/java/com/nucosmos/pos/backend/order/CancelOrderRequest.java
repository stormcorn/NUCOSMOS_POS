package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.Size;

public record CancelOrderRequest(
        @Size(max = 255) String reason
) {
}
