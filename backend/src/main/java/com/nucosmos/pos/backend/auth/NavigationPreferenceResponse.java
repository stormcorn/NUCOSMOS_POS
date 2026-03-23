package com.nucosmos.pos.backend.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record NavigationPreferenceResponse(
        List<String> rootOrder,
        Map<String, List<String>> childOrders,
        OffsetDateTime updatedAt
) {
}
