import type { PermissionKey } from "@/constants/permissions";

export type LoginRequest = {
  storeCode: string;
  roleCode?: string;
  pin: string;
  deviceCode?: string;
};

export type RegistrationStartRequest = {
  storeCode: string;
  phoneNumber: string;
  pin: string;
};

export type RegistrationStartResponse = {
  registrationId: string;
  storeCode: string;
  phoneNumber: string;
  provider: string;
  status: string;
  expiresAt: string;
};

export type RegistrationCompleteRequest = {
  registrationId: string;
  verificationCode: string;
  firebaseIdToken?: string;
};

export type RegistrationCompleteResponse = {
  userId: string;
  employeeCode: string;
  displayName: string;
  storeCode: string;
  phoneNumber: string;
  status: string;
  activatedAt: string;
};

export type AuthStore = {
  code: string;
  name: string;
  receiptFooterText?: string;
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
