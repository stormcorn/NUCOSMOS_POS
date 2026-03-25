import { apiRequest } from "@/api/http";
import type {
  ManufacturedAdminItem,
  ManufacturedLotItem,
  ManufacturedMovementItem,
  ManufacturedMovementRequest,
  ManufacturedUpsertRequest,
} from "@/types/manufactured";

export function fetchManufacturedItems() {
  return apiRequest<ManufacturedAdminItem[]>("/api/v1/admin/manufactured-items");
}

export function fetchManufacturedMovements() {
  return apiRequest<ManufacturedMovementItem[]>("/api/v1/admin/manufactured-items/movements");
}

export function fetchManufacturedLots() {
  return apiRequest<ManufacturedLotItem[]>("/api/v1/admin/manufactured-items/lots");
}

export function createManufacturedItem(payload: ManufacturedUpsertRequest) {
  return apiRequest<ManufacturedAdminItem>("/api/v1/admin/manufactured-items", {
    method: "POST",
    body: payload,
  });
}

export function updateManufacturedItem(manufacturedItemId: string, payload: ManufacturedUpsertRequest) {
  return apiRequest<ManufacturedAdminItem>(`/api/v1/admin/manufactured-items/${manufacturedItemId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivateManufacturedItem(manufacturedItemId: string) {
  return apiRequest<ManufacturedAdminItem>(`/api/v1/admin/manufactured-items/${manufacturedItemId}/deactivate`, {
    method: "POST",
  });
}

export function createManufacturedMovement(manufacturedItemId: string, payload: ManufacturedMovementRequest) {
  return apiRequest<ManufacturedMovementItem>(`/api/v1/admin/manufactured-items/${manufacturedItemId}/movements`, {
    method: "POST",
    body: payload,
  });
}
