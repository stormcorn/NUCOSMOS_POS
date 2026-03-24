package com.nucosmos.pos.backend.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;
import java.util.List;

public record OrderCreateItemRequest(
        @NotNull UUID productId,
        @Min(1) int quantity,
        @Size(max = 255) String note,
        List<UUID> selectedOptionIds
) {
}
