import { apiRequest } from "@/api/http";
import type { InventoryMovementItem, InventoryMovementRequest, InventoryStockItem } from "@/types/inventory";

export function fetchInventoryStocks(lowStockOnly = false) {
  return apiRequest<InventoryStockItem[]>("/api/v1/admin/inventory/stocks", {
    query: { lowStockOnly },
  });
}

export function fetchInventoryMovements() {
  return apiRequest<InventoryMovementItem[]>("/api/v1/admin/inventory/movements");
}

export function createInventoryMovement(payload: InventoryMovementRequest) {
  return apiRequest<InventoryMovementItem>("/api/v1/admin/inventory/movements", {
    method: "POST",
    body: payload,
  });
}

export function updateInventoryReorderLevel(productId: string, reorderLevel: number) {
  return apiRequest<InventoryStockItem>(`/api/v1/admin/inventory/stocks/${productId}/reorder-level`, {
    method: "PUT",
    body: { reorderLevel },
  });
}
