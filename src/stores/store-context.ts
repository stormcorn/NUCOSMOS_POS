import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { fetchStores } from "@/api/devices";
import { ApiError } from "@/api/http";
import { useAuthStore } from "@/stores/auth";
import type { StoreSummary } from "@/types/store";

export const useStoreContextStore = defineStore("storeContext", () => {
  const stores = ref<StoreSummary[]>([]);
  const selectedStoreCode = ref("");
  const loading = ref(false);
  const errorMessage = ref("");

  const selectedStore = computed(
    () => stores.value.find((store) => store.code === selectedStoreCode.value) ?? null,
  );

  function syncSelectedStore() {
    const authStore = useAuthStore();

    if (!selectedStoreCode.value && authStore.currentStoreCode) {
      selectedStoreCode.value = authStore.currentStoreCode;
    }

    if (!selectedStoreCode.value && stores.value[0]) {
      selectedStoreCode.value = stores.value[0].code;
    }
  }

  async function loadStores() {
    loading.value = true;
    errorMessage.value = "";

    try {
      stores.value = await fetchStores();
      syncSelectedStore();
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法載入門市資料";
    } finally {
      loading.value = false;
    }
  }

  function setSelectedStoreCode(storeCode: string) {
    selectedStoreCode.value = storeCode;
  }

  return {
    errorMessage,
    loadStores,
    loading,
    selectedStore,
    selectedStoreCode,
    setSelectedStoreCode,
    stores,
    syncSelectedStore,
  };
});
