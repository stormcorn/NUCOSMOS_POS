package com.nucosmos.pos.backend.supply;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PackagingMovementRequest(
        @NotBlank String movementType,
        @NotNull @Min(1) Integer quantity,
        @DecimalMin("0.0") BigDecimal unitCost,
        @Size(max = 255) String note
) {
}
