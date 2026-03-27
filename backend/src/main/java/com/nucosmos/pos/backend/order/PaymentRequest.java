package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotBlank String paymentMethod,
        @DecimalMin(value = "0.00") BigDecimal amount,
        @DecimalMin(value = "0.00") BigDecimal amountReceived,
        @Size(max = 255) String note
) {
}
