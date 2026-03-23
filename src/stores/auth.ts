import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { fetchCurrentSession, loginWithPin } from "@/api/auth";
import { ApiError, setStoredAccessToken } from "@/api/http";
import type { PermissionKey } from "@/constants/permissions";
import type { CurrentSession, LoginRequest } from "@/types/auth";

const ACCESS_TOKEN_KEY = "nucosmos.admin.accessToken";
const EXPIRES_AT_KEY = "nucosmos.admin.expiresAt";

export const useAuthStore = defineStore("auth", () => {
  const accessToken = ref<string | null>(window.localStorage.getItem(ACCESS_TOKEN_KEY));
  const expiresAt = ref<string | null>(window.localStorage.getItem(EXPIRES_AT_KEY));
  const session = ref<CurrentSession | null>(null);
  const initialized = ref(false);
  const bootstrapPromise = ref<Promise<void> | null>(null);
  const loading = ref(false);
  const errorMessage = ref("");

  const isAuthenticated = computed(() => Boolean(accessToken.value && session.value));
  const userDisplayName = computed(() => session.value?.displayName ?? "未登入");
  const activeRole = computed(() => session.value?.activeRole ?? "");
  const currentStoreCode = computed(() => session.value?.storeCode ?? "");
  const permissionKeys = computed(() => session.value?.permissionKeys ?? []);

  function hasPermission(permissionKey: PermissionKey | string) {
    return permissionKeys.value.includes(permissionKey as PermissionKey);
  }

  function hasAnyPermission(requiredPermissionKeys: readonly string[] = []) {
    if (requiredPermissionKeys.length === 0) {
      return true;
    }

    return requiredPermissionKeys.some((permissionKey) => hasPermission(permissionKey));
  }

  function persistToken(token: string | null, nextExpiresAt: string | null) {
    accessToken.value = token;
    expiresAt.value = nextExpiresAt;
    setStoredAccessToken(token);

    if (nextExpiresAt) {
      window.localStorage.setItem(EXPIRES_AT_KEY, nextExpiresAt);
    } else {
      window.localStorage.removeItem(EXPIRES_AT_KEY);
    }
  }

  function clearSession() {
    persistToken(null, null);
    session.value = null;
    errorMessage.value = "";
  }

  async function restoreSession() {
    if (!accessToken.value) {
      initialized.value = true;
      return;
    }

    try {
      session.value = await fetchCurrentSession();
    } catch (error) {
      clearSession();
      if (error instanceof ApiError && error.status !== 401) {
        errorMessage.value = error.message;
      }
    } finally {
      initialized.value = true;
    }
  }

  async function bootstrap() {
    if (initialized.value) {
      return;
    }

    if (!bootstrapPromise.value) {
      bootstrapPromise.value = restoreSession().finally(() => {
        bootstrapPromise.value = null;
      });
    }

    return bootstrapPromise.value;
  }

  async function signIn(payload: LoginRequest) {
    loading.value = true;
    errorMessage.value = "";

    try {
      const response = await loginWithPin(payload);
      persistToken(response.accessToken, response.expiresAt);
      session.value = await fetchCurrentSession();
      initialized.value = true;
      return true;
    } catch (error) {
      clearSession();
      errorMessage.value = error instanceof ApiError ? error.message : "登入失敗";
      return false;
    } finally {
      loading.value = false;
    }
  }

  function signOut() {
    clearSession();
    initialized.value = true;
  }

  return {
    accessToken,
    activeRole,
    bootstrap,
    currentStoreCode,
    errorMessage,
    expiresAt,
    hasAnyPermission,
    hasPermission,
    initialized,
    isAuthenticated,
    loading,
    permissionKeys,
    session,
    signIn,
    signOut,
    userDisplayName,
  };
});
