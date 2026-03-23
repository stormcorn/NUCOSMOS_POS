package com.nucosmos.pos.backend.supply;

import jakarta.validation.constraints.Size;

public record PurchaseOrderReceiveRequest(
        @Size(max = 500) String note
) {
}
