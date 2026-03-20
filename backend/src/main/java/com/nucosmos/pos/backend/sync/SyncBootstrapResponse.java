package com.nucosmos.pos.backend.sync;

import java.time.OffsetDateTime;
import java.util.List;

public record SyncBootstrapResponse(
        String storeCode,
        String deviceCode,
        String deviceStatus,
        boolean deviceAuthorized,
        OffsetDateTime serverTime,
        List<SyncCategoryResponse> categories,
        List<SyncProductResponse> products
) {
}
