import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { fetchStorageStatus } from "@/api/system";
import { ApiError } from "@/api/http";
import type { StorageStatus } from "@/types/system";

export const useSystemStore = defineStore("system", () => {
  const storageStatus = ref<StorageStatus | null>(null);
  const loading = ref(false);
  const errorMessage = ref("");

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
      errorMessage.value = error instanceof ApiError ? error.message : "無法取得伺服器磁碟狀態";
    } finally {
      loading.value = false;
    }
  }

  return {
    errorMessage,
    hasStorageWarning,
    isStorageCritical,
    loadStorageStatus,
    loading,
    storageStatus,
  };
});
