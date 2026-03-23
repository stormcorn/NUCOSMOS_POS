package com.nucosmos.pos.backend.order;

import java.util.UUID;

public record RefundItemResponse(
        UUID id,
        UUID orderItemId,
        UUID productId,
        String productSku,
        String productName,
        int quantity,
        String inventoryDisposition
) {
}
