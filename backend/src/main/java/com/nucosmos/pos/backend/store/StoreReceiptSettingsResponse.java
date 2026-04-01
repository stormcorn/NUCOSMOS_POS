package com.nucosmos.pos.backend.store;

import java.util.UUID;

public record StoreReceiptSettingsResponse(
        UUID storeId,
        String storeCode,
        String storeName,
        String receiptFooterText
) {
}
