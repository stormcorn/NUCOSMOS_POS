package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.Size;

public record CardCaptureRequest(
        @Size(max = 255) String note
) {
}
