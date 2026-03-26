import { apiRequest } from "@/api/http";
import type {
  ClearPendingPhoneRegistrationRequest,
  ClearPendingPhoneRegistrationResponse,
  PermissionDefinition,
  RoleAdminItem,
  RoleAdminRequest,
  UserAdminItem,
  UserAdminRequest,
} from "@/types/access";

export function fetchAdminUsers(status?: string, storeCode?: string) {
  return apiRequest<UserAdminItem[]>("/api/v1/admin/access/users", {
    query: { status, storeCode },
  });
}

export function createAdminUser(payload: UserAdminRequest) {
  return apiRequest<UserAdminItem>("/api/v1/admin/access/users", {
    method: "POST",
    body: payload,
  });
}

export function updateAdminUser(userId: string, payload: UserAdminRequest) {
  return apiRequest<UserAdminItem>(`/api/v1/admin/access/users/${userId}`, {
    method: "PUT",
    body: payload,
  });
}

export function clearPendingPhoneRegistrations(payload: ClearPendingPhoneRegistrationRequest) {
  return apiRequest<ClearPendingPhoneRegistrationResponse>("/api/v1/admin/access/phone-registrations/clear-pending", {
    method: "POST",
    body: payload,
  });
}

export function fetchAdminRoles() {
  return apiRequest<RoleAdminItem[]>("/api/v1/admin/access/roles");
}

export function createAdminRole(payload: RoleAdminRequest) {
  return apiRequest<RoleAdminItem>("/api/v1/admin/access/roles", {
    method: "POST",
    body: payload,
  });
}

export function updateAdminRole(roleId: string, payload: RoleAdminRequest) {
  return apiRequest<RoleAdminItem>(`/api/v1/admin/access/roles/${roleId}`, {
    method: "PUT",
    body: payload,
  });
}

export function fetchPermissionCatalog() {
  return apiRequest<PermissionDefinition[]>("/api/v1/admin/access/permissions");
}
