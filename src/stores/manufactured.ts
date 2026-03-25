import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { ApiError } from "@/api/http";
import {
  createManufacturedItem,
  createManufacturedMovement,
  deactivateManufacturedItem,
  fetchManufacturedItems,
  fetchManufacturedLots,
  fetchManufacturedMovements,
  updateManufacturedItem,
} from "@/api/manufactured";
import type {
  ManufacturedAdminItem,
  ManufacturedLotItem,
  ManufacturedMovementItem,
  ManufacturedMovementRequest,
  ManufacturedUpsertRequest,
} from "@/types/manufactured";

export const useManufacturedStore = defineStore("manufactured", () => {
  const items = ref<ManufacturedAdminItem[]>([]);
  const lots = ref<ManufacturedLotItem[]>([]);
  const movements = ref<ManufacturedMovementItem[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");

  const activeItems = computed(() => items.value.filter((item) => item.active));

  async function loadManufactured() {
    loading.value = true;
    errorMessage.value = "";

    try {
      const [nextItems, nextMovements, nextLots] = await Promise.all([
        fetchManufacturedItems(),
        fetchManufacturedMovements(),
        fetchManufacturedLots(),
      ]);
      items.value = nextItems;
      movements.value = nextMovements;
      lots.value = nextLots;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法載入製成品資料。";
    } finally {
      loading.value = false;
    }
  }

  async function createItem(payload: ManufacturedUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await createManufacturedItem(payload);
      await loadManufactured();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立製成品失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function updateItem(manufacturedItemId: string, payload: ManufacturedUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await updateManufacturedItem(manufacturedItemId, payload);
      await loadManufactured();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新製成品失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deactivateItem(manufacturedItemId: string) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await deactivateManufacturedItem(manufacturedItemId);
      await loadManufactured();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "停用製成品失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function submitMovement(manufacturedItemId: string, payload: ManufacturedMovementRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await createManufacturedMovement(manufacturedItemId, payload);
      await loadManufactured();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立製成品異動失敗。";
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
    loadManufactured,
    loading,
    movements,
    saving,
    submitMovement,
    updateItem,
  };
});
