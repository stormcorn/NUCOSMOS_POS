package com.nucosmos.pos.backend.report;

public record MovementTotalResponse(
        String scope,
        String movementType,
        int entryCount,
        int totalQuantity,
        int netDelta
) {
}
