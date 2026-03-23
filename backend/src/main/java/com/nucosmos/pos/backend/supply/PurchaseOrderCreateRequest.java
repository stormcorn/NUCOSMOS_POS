package com.nucosmos.pos.backend.supply;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderCreateRequest(
        @NotNull UUID supplierId,
        @Size(max = 500) String note,
        OffsetDateTime expectedAt,
        @NotEmpty List<@Valid PurchaseOrderLineRequest> lines
) {
}
