import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { ApiError } from "@/api/http";
import {
  createAdminRole,
  createAdminUser,
  fetchAdminRoles,
  fetchAdminUsers,
  fetchPermissionCatalog,
  updateAdminRole,
  updateAdminUser,
} from "@/api/access";
import type {
  PermissionDefinition,
  RoleAdminItem,
  RoleAdminRequest,
  UserAdminItem,
  UserAdminRequest,
} from "@/types/access";

export const useAccessControlStore = defineStore("accessControl", () => {
  const users = ref<UserAdminItem[]>([]);
  const roles = ref<RoleAdminItem[]>([]);
  const permissions = ref<PermissionDefinition[]>([]);
  const loadingUsers = ref(false);
  const loadingRoles = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");
  const userStatusFilter = ref("");
  const userStoreFilter = ref("");

  const permissionsByGroup = computed(() => {
    const groups = new Map<string, PermissionDefinition[]>();

    for (const permission of permissions.value) {
      const existing = groups.get(permission.groupName) ?? [];
      existing.push(permission);
      groups.set(permission.groupName, existing);
    }

    return Array.from(groups.entries()).map(([groupName, items]) => ({
      groupName,
      items,
    }));
  });

  async function loadUsers(status = userStatusFilter.value, storeCode = userStoreFilter.value) {
    loadingUsers.value = true;
    errorMessage.value = "";
    userStatusFilter.value = status;
    userStoreFilter.value = storeCode;

    try {
      users.value = await fetchAdminUsers(status || undefined, storeCode || undefined);
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入使用者資料時發生錯誤。";
    } finally {
      loadingUsers.value = false;
    }
  }

  async function loadRoles() {
    loadingRoles.value = true;
    errorMessage.value = "";

    try {
      roles.value = await fetchAdminRoles();
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入角色資料時發生錯誤。";
    } finally {
      loadingRoles.value = false;
    }
  }

  async function loadPermissions() {
    errorMessage.value = "";

    try {
      permissions.value = await fetchPermissionCatalog();
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入權限目錄時發生錯誤。";
    }
  }

  async function bootstrap() {
    await Promise.all([loadUsers(), loadRoles(), loadPermissions()]);
  }

  async function createUser(payload: UserAdminRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await createAdminUser(payload);
      await loadUsers();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立使用者失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function updateUser(userId: string, payload: UserAdminRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await updateAdminUser(userId, payload);
      await loadUsers();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新使用者失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function createRole(payload: RoleAdminRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await createAdminRole(payload);
      await loadRoles();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立角色失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function updateRole(roleId: string, payload: RoleAdminRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await updateAdminRole(roleId, payload);
      await loadRoles();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新角色失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    bootstrap,
    createRole,
    createUser,
    errorMessage,
    loadingRoles,
    loadingUsers,
    loadPermissions,
    loadRoles,
    loadUsers,
    permissions,
    permissionsByGroup,
    roles,
    saving,
    updateRole,
    updateUser,
    userStatusFilter,
    userStoreFilter,
    users,
  };
});
