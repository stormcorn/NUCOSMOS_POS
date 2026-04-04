package com.nucosmos.pos.backend.order;

import java.time.OffsetDateTime;
import java.util.List;

public record BulkDeleteTestOrdersResponse(
        OffsetDateTime from,
        OffsetDateTime to,
        int matchedCount,
        int deletedCount,
        int inventoryRestoredCount,
        int skippedCount,
        List<String> skippedOrderNumbers
) {
}
