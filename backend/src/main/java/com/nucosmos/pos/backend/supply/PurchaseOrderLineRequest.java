package com.nucosmos.pos.backend.supply;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PurchaseOrderLineRequest(
        @NotBlank @Size(max = 20) String itemType,
        @NotNull UUID itemId,
        @NotNull @Min(1) Integer orderedQuantity,
        @DecimalMin("0.0") BigDecimal unitCost,
        @Size(max = 80) String batchCode,
        OffsetDateTime expiryDate,
        OffsetDateTime manufacturedAt,
        @Size(max = 255) String note
) {
}
