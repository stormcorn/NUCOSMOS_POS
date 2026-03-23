package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RefundRequest(
        @NotNull UUID paymentId,
        @NotNull @Size(max = 30) String refundMethod,
        @DecimalMin(value = "0.01") BigDecimal amount,
        @Size(max = 255) String reason,
        List<RefundItemRequest> items
) {
}
