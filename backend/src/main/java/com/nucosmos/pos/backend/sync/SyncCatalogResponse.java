package com.nucosmos.pos.backend.sync;

import java.time.OffsetDateTime;
import java.util.List;

public record SyncCatalogResponse(
        String storeCode,
        String deviceCode,
        OffsetDateTime since,
        OffsetDateTime serverTime,
        List<SyncCategoryResponse> categories,
        List<SyncProductResponse> products
) {
}
