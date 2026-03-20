package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CardAuthorizationRequest(
        @DecimalMin(value = "0.01") BigDecimal amount,
        @Size(max = 255) String note
) {
}
