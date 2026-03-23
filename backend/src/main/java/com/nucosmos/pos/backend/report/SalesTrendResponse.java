package com.nucosmos.pos.backend.report;

import java.time.OffsetDateTime;
import java.util.List;

public record SalesTrendResponse(
        String storeCode,
        OffsetDateTime from,
        OffsetDateTime to,
        String granularity,
        List<SalesTrendPointResponse> points
) {
}
