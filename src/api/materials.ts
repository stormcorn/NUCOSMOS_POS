import { apiRequest } from "@/api/http";
import type {
  MaterialAdminItem,
  MaterialLotItem,
  MaterialMovementItem,
  MaterialMovementRequest,
  MaterialUpsertRequest,
} from "@/types/materials";

export function fetchMaterials() {
  return apiRequest<MaterialAdminItem[]>("/api/v1/admin/materials");
}

export function fetchMaterialMovements() {
  return apiRequest<MaterialMovementItem[]>("/api/v1/admin/materials/movements");
}

export function fetchMaterialLots() {
  return apiRequest<MaterialLotItem[]>("/api/v1/admin/materials/lots");
}

export function createMaterial(payload: MaterialUpsertRequest) {
  return apiRequest<MaterialAdminItem>("/api/v1/admin/materials", {
    method: "POST",
    body: payload,
  });
}

export function updateMaterial(materialId: string, payload: MaterialUpsertRequest) {
  return apiRequest<MaterialAdminItem>(`/api/v1/admin/materials/${materialId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivateMaterial(materialId: string) {
  return apiRequest<MaterialAdminItem>(`/api/v1/admin/materials/${materialId}/deactivate`, {
    method: "POST",
  });
}

export function createMaterialMovement(materialId: string, payload: MaterialMovementRequest) {
  return apiRequest<MaterialMovementItem>(`/api/v1/admin/materials/${materialId}/movements`, {
    method: "POST",
    body: payload,
  });
}
