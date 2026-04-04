import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { cleanupDockerCaches, fetchDockerMaintenanceStatus, fetchStorageStatus } from "@/api/system";
import { ApiError } from "@/api/http";
import type { DockerMaintenanceCleanup, DockerMaintenanceStatus, StorageStatus } from "@/types/system";

export const useSystemStore = defineStore("system", () => {
  const storageStatus = ref<StorageStatus | null>(null);
  const loading = ref(false);
  const errorMessage = ref("");
  const maintenanceStatus = ref<DockerMaintenanceStatus | null>(null);
  const maintenanceLoading = ref(false);
  const maintenanceActionLoading = ref(false);
  const maintenanceErrorMessage = ref("");
  const cleanupResult = ref<DockerMaintenanceCleanup | null>(null);

  const hasStorageWarning = computed(
    () => storageStatus.value?.level === "WARNING" || storageStatus.value?.level === "CRITICAL",
  );
  const isStorageCritical = computed(() => storageStatus.value?.level === "CRITICAL");

  async function loadStorageStatus() {
    loading.value = true;
    try {
      storageStatus.value = await fetchStorageStatus();
      errorMessage.value = "";
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "Failed to load storage status.";
    } finally {
      loading.value = false;
    }
  }

  async function loadDockerMaintenanceStatus() {
    maintenanceLoading.value = true;
    try {
      maintenanceStatus.value = await fetchDockerMaintenanceStatus();
      maintenanceErrorMessage.value = "";
    } catch (error) {
      maintenanceErrorMessage.value = error instanceof ApiError ? error.message : "Failed to load cleanup status.";
    } finally {
      maintenanceLoading.value = false;
    }
  }

  async function runDockerCacheCleanup() {
    maintenanceActionLoading.value = true;
    try {
      const result = await cleanupDockerCaches();
      cleanupResult.value = result;
      maintenanceErrorMessage.value = "";
      if (maintenanceStatus.value) {
        maintenanceStatus.value = {
          ...maintenanceStatus.value,
          details: result.afterDetails,
          summary: result.summary,
        };
      }
      return result;
    } catch (error) {
      maintenanceErrorMessage.value = error instanceof ApiError ? error.message : "Failed to clean Docker cache.";
      throw error;
    } finally {
      maintenanceActionLoading.value = false;
    }
  }

  return {
    cleanupResult,
    errorMessage,
    hasStorageWarning,
    isStorageCritical,
    loadDockerMaintenanceStatus,
    loadStorageStatus,
    loading,
    maintenanceActionLoading,
    maintenanceErrorMessage,
    maintenanceLoading,
    maintenanceStatus,
    runDockerCacheCleanup,
    storageStatus,
  };
});
