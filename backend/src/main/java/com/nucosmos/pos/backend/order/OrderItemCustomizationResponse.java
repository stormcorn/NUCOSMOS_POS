package com.nucosmos.pos.backend.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemCustomizationResponse(
        UUID id,
        String groupName,
        String optionName,
        BigDecimal priceDelta,
        int lineNumber
) {
}
