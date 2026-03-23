import { apiRequest } from "@/api/http";
import type {
  PackagingAdminItem,
  PackagingMovementItem,
  PackagingMovementRequest,
  PackagingUpsertRequest,
} from "@/types/packaging";

export function fetchPackagingItems() {
  return apiRequest<PackagingAdminItem[]>("/api/v1/admin/packaging-items");
}

export function fetchPackagingMovements() {
  return apiRequest<PackagingMovementItem[]>("/api/v1/admin/packaging-items/movements");
}

export function createPackagingItem(payload: PackagingUpsertRequest) {
  return apiRequest<PackagingAdminItem>("/api/v1/admin/packaging-items", {
    method: "POST",
    body: payload,
  });
}

export function updatePackagingItem(packagingItemId: string, payload: PackagingUpsertRequest) {
  return apiRequest<PackagingAdminItem>(`/api/v1/admin/packaging-items/${packagingItemId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivatePackagingItem(packagingItemId: string) {
  return apiRequest<PackagingAdminItem>(`/api/v1/admin/packaging-items/${packagingItemId}/deactivate`, {
    method: "POST",
  });
}

export function createPackagingMovement(packagingItemId: string, payload: PackagingMovementRequest) {
  return apiRequest<PackagingMovementItem>(`/api/v1/admin/packaging-items/${packagingItemId}/movements`, {
    method: "POST",
    body: payload,
  });
}
