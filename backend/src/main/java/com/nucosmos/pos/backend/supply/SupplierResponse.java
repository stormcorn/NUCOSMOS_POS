package com.nucosmos.pos.backend.supply;

import java.util.UUID;

public record SupplierResponse(
        UUID id,
        String code,
        String name,
        String contactName,
        String phone,
        String email,
        String note,
        boolean active
) {
}
