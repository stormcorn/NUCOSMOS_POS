import { apiRequest } from "@/api/http";
import type {
  DefectiveInventoryActionRequest,
  InventoryMovementItem,
  InventoryMovementRequest,
  InventoryStockItem,
} from "@/types/inventory";

export function fetchInventoryStocks(lowStockOnly = false) {
  return apiRequest<InventoryStockItem[]>("/api/v1/admin/inventory/stocks", {
    query: { lowStockOnly },
  });
}

export function fetchInventoryMovements() {
  return apiRequest<InventoryMovementItem[]>("/api/v1/admin/inventory/movements");
}

export function fetchDefectiveInventoryStocks() {
  return apiRequest<InventoryStockItem[]>("/api/v1/admin/inventory/defective/stocks");
}

export function fetchDefectiveInventoryMovements() {
  return apiRequest<InventoryMovementItem[]>("/api/v1/admin/inventory/defective/movements");
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

export function scrapDefectiveInventory(productId: string, payload: DefectiveInventoryActionRequest) {
  return apiRequest<InventoryMovementItem>(`/api/v1/admin/inventory/defective/${productId}/scrap`, {
    method: "POST",
    body: payload,
  });
}

export function restoreDefectiveInventory(productId: string, payload: DefectiveInventoryActionRequest) {
  return apiRequest<InventoryMovementItem>(`/api/v1/admin/inventory/defective/${productId}/restore`, {
    method: "POST",
    body: payload,
  });
}
