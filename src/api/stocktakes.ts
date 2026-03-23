import { apiRequest } from "@/api/http";
import type { StocktakeCreateRequest, StocktakeRecord } from "@/types/stocktake";

export function fetchInventoryStocktakes() {
  return apiRequest<StocktakeRecord[]>("/api/v1/admin/inventory/stocktakes");
}

export function createInventoryStocktake(payload: StocktakeCreateRequest) {
  return apiRequest<StocktakeRecord>("/api/v1/admin/inventory/stocktakes", {
    method: "POST",
    body: payload,
  });
}
