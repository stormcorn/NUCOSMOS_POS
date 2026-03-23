import type { PermissionKey } from "@/constants/permissions";

export type RoleCode = "MANAGER" | "ADMIN";

export type LoginRequest = {
  storeCode: string;
  roleCode: RoleCode;
  pin: string;
  deviceCode?: string;
};

export type AuthStore = {
  code: string;
  name: string;
};

export type AuthStaff = {
  id: string;
  employeeCode: string;
  displayName: string;
  roleCodes: string[];
  activeRole: string;
  permissionKeys: PermissionKey[];
};

export type LoginResponse = {
  tokenType: string;
  accessToken: string;
  expiresAt: string;
  deviceCode: string | null;
  store: AuthStore;
  staff: AuthStaff;
};

export type CurrentSession = {
  userId: string;
  employeeCode: string;
  displayName: string;
  storeCode: string;
  activeRole: string;
  roleCodes: string[];
  permissionKeys: PermissionKey[];
  deviceCode: string | null;
};
