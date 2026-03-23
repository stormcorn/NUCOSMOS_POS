package com.nucosmos.pos.backend.inventory.stocktake;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InventoryStocktakeCreateRequest(
        @Size(max = 255) String note,
        @NotEmpty List<@Valid InventoryStocktakeCreateItemRequest> items
) {
}
