package com.nucosmos.pos.backend.shift;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ShiftCloseRequest(
        @NotNull @DecimalMin(value = "0.00") BigDecimal closingCashAmount,
        @Size(max = 255) String note
) {
}
