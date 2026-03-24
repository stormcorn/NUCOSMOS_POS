package com.nucosmos.pos.backend.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductCustomizationGroupRequest(
        @NotBlank @Size(max = 80) String name,
        @NotBlank String selectionMode,
        boolean required,
        @Min(0) int minSelections,
        @Min(1) int maxSelections,
        @Min(0) int displayOrder,
        @NotEmpty List<@Valid ProductCustomizationOptionRequest> options
) {
}
