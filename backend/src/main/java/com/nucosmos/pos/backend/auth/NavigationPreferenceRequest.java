package com.nucosmos.pos.backend.auth;

import java.util.List;
import java.util.Map;

public record NavigationPreferenceRequest(
        List<String> rootOrder,
        Map<String, List<String>> childOrders
) {
}
