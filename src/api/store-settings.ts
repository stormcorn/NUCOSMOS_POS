import { apiRequest } from "@/api/http";
import type { StoreReceiptSettings } from "@/types/store";

export function fetchStoreReceiptSettings(storeId: string) {
  return apiRequest<StoreReceiptSettings>(`/api/v1/admin/stores/${storeId}/receipt-settings`);
}

export function updateStoreReceiptSettings(storeId: string, receiptFooterText: string) {
  return apiRequest<StoreReceiptSettings>(`/api/v1/admin/stores/${storeId}/receipt-settings`, {
    method: "PUT",
    body: { receiptFooterText },
  });
}
