package com.nucosmos.pos.backend.store;

import jakarta.validation.constraints.Size;

public record StoreReceiptSettingsRequest(
        @Size(max = 1000) String receiptFooterText
) {
}
