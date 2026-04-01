package com.nucosmos.pos.backend.store;

import java.util.UUID;

public record StoreSummaryResponse(
        UUID id,
        String code,
        String name,
        String timezone,
        String currencyCode,
        String status,
        String receiptFooterText
) {
}
