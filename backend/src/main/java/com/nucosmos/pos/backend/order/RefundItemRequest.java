package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RefundItemRequest(
        @NotNull UUID orderItemId,
        @Min(1) int quantity,
        @Size(max = 20) String inventoryDisposition
) {
}
