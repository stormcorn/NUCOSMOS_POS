package com.nucosmos.pos.backend.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreateRequest(
        @NotEmpty List<@Valid OrderCreateItemRequest> items,
        @Size(max = 500) String note,
        @DecimalMin(value = "0.00") BigDecimal discountAmount,
        @Size(max = 255) String discountNote
) {
}
