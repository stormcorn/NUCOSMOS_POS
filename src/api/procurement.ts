import { apiRequest } from "@/api/http";
import type {
  PurchaseOrder,
  PurchaseOrderCreateRequest,
  ReplenishmentSuggestion,
  SupplierItem,
  SupplierUpsertRequest,
} from "@/types/procurement";

export function fetchSuppliers() {
  return apiRequest<SupplierItem[]>("/api/v1/admin/suppliers");
}

export function createSupplier(payload: SupplierUpsertRequest) {
  return apiRequest<SupplierItem>("/api/v1/admin/suppliers", {
    method: "POST",
    body: payload,
  });
}

export function updateSupplier(supplierId: string, payload: SupplierUpsertRequest) {
  return apiRequest<SupplierItem>(`/api/v1/admin/suppliers/${supplierId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivateSupplier(supplierId: string) {
  return apiRequest<SupplierItem>(`/api/v1/admin/suppliers/${supplierId}/deactivate`, {
    method: "POST",
  });
}

export function fetchReplenishmentSuggestions() {
  return apiRequest<ReplenishmentSuggestion[]>("/api/v1/admin/replenishment-suggestions");
}

export function fetchPurchaseOrders() {
  return apiRequest<PurchaseOrder[]>("/api/v1/admin/purchase-orders");
}

export function createPurchaseOrder(payload: PurchaseOrderCreateRequest) {
  return apiRequest<PurchaseOrder>("/api/v1/admin/purchase-orders", {
    method: "POST",
    body: payload,
  });
}

export function receivePurchaseOrder(purchaseOrderId: string, note?: string) {
  return apiRequest<PurchaseOrder>(`/api/v1/admin/purchase-orders/${purchaseOrderId}/receive`, {
    method: "POST",
    body: { note },
  });
}
