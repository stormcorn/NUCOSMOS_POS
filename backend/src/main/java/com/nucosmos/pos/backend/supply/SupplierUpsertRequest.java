package com.nucosmos.pos.backend.supply;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierUpsertRequest(
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 120) String contactName,
        @Size(max = 50) String phone,
        @Size(max = 120) String email,
        @Size(max = 500) String note
) {
}
