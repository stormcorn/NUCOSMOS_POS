package com.nucosmos.pos.backend.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DefectiveInventoryActionRequest(
        @NotNull @Min(1) Integer quantity,
        @NotBlank @Size(max = 50) String reasonCode,
        @Size(max = 255) String note
) {
}
