package com.nucosmos.pos.backend.shift;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ShiftOpenRequest(
        @NotNull @DecimalMin(value = "0.00") BigDecimal openingCashAmount,
        @Size(max = 255) String note
) {
}
