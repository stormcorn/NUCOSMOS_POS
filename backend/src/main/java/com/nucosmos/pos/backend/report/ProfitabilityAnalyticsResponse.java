package com.nucosmos.pos.backend.report;

import java.time.OffsetDateTime;
import java.util.List;

public record ProfitabilityAnalyticsResponse(
        String storeCode,
        OffsetDateTime from,
        OffsetDateTime to,
        CostTransferSummaryResponse costTransferSummary,
        List<ProductProfitabilityResponse> topProductsByGrossProfit,
        List<ProductProfitabilityResponse> lowestProductsByMargin,
        List<CategoryProfitabilityResponse> categoryProfitability,
        List<OrderProfitabilityResponse> topOrdersByGrossProfit,
        List<OrderProfitabilityResponse> lowestOrdersByMargin
) {
}
