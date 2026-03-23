import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { ApiError } from "@/api/http";
import {
  createPackagingItem,
  createPackagingMovement,
  deactivatePackagingItem,
  fetchPackagingItems,
  fetchPackagingLots,
  fetchPackagingMovements,
  updatePackagingItem,
} from "@/api/packaging";
import type {
  PackagingAdminItem,
  PackagingLotItem,
  PackagingMovementItem,
  PackagingMovementRequest,
  PackagingUpsertRequest,
} from "@/types/packaging";

export const usePackagingStore = defineStore("packaging", () => {
  const items = ref<PackagingAdminItem[]>([]);
  const lots = ref<PackagingLotItem[]>([]);
  const movements = ref<PackagingMovementItem[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");

  const activeItems = computed(() => items.value.filter((item) => item.active));

  async function loadPackaging() {
    loading.value = true;
    errorMessage.value = "";

    try {
      const [nextItems, nextMovements, nextLots] = await Promise.all([
        fetchPackagingItems(),
        fetchPackagingMovements(),
        fetchPackagingLots(),
      ]);
      items.value = nextItems;
      movements.value = nextMovements;
      lots.value = nextLots;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入包裝資料失敗。";
    } finally {
      loading.value = false;
    }
  }

  async function createItem(payload: PackagingUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await createPackagingItem(payload);
      await loadPackaging();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立包裝失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function updateItem(packagingItemId: string, payload: PackagingUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await updatePackagingItem(packagingItemId, payload);
      await loadPackaging();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新包裝失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deactivateItem(packagingItemId: string) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await deactivatePackagingItem(packagingItemId);
      await loadPackaging();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "停用包裝失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function submitMovement(packagingItemId: string, payload: PackagingMovementRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await createPackagingMovement(packagingItemId, payload);
      await loadPackaging();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立包裝異動失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    activeItems,
    createItem,
    deactivateItem,
    errorMessage,
    items,
    lots,
    loadPackaging,
    loading,
    movements,
    saving,
    submitMovement,
    updateItem,
  };
});
