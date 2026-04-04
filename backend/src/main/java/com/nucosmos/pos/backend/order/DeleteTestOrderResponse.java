package com.nucosmos.pos.backend.order;

import java.util.UUID;

public record DeleteTestOrderResponse(
        UUID orderId,
        String orderNumber,
        boolean inventoryRestored
) {
}
