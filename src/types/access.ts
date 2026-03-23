export type PermissionDefinition = {
  key: string;
  label: string;
  groupName: string;
  description: string;
};

export type UserAdminItem = {
  id: string;
  employeeCode: string;
  displayName: string;
  status: string;
  lastLoginAt: string | null;
  roleCodes: string[];
  storeCodes: string[];
  storeNames: string[];
};

export type UserAdminRequest = {
  employeeCode: string;
  displayName: string;
  pin: string;
  status: string;
  roleCodes: string[];
  storeCodes: string[];
};

export type RoleAdminItem = {
  id: string;
  code: string;
  name: string;
  description: string | null;
  active: boolean;
  permissionKeys: string[];
};

export type RoleAdminRequest = {
  code: string;
  name: string;
  description: string;
  active: boolean;
  permissionKeys: string[];
};
