package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReceiptPrizeAdminRequest(
        @NotBlank
        @Size(max = 120)
        String name,
        @Size(max = 240)
        String description,
        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.00")
        BigDecimal probabilityPercent,
        @Min(0)
        int remainingQuantity,
        boolean active,
        @Min(0)
        int displayOrder
) {
}
