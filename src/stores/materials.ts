import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { ApiError } from "@/api/http";
import {
  createMaterial,
  createMaterialMovement,
  deactivateMaterial,
  fetchMaterialMovements,
  fetchMaterials,
  updateMaterial,
} from "@/api/materials";
import type {
  MaterialAdminItem,
  MaterialMovementItem,
  MaterialMovementRequest,
  MaterialUpsertRequest,
} from "@/types/materials";

export const useMaterialsStore = defineStore("materials", () => {
  const items = ref<MaterialAdminItem[]>([]);
  const movements = ref<MaterialMovementItem[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");

  const activeItems = computed(() => items.value.filter((item) => item.active));

  async function loadMaterials() {
    loading.value = true;
    errorMessage.value = "";

    try {
      const [nextItems, nextMovements] = await Promise.all([
        fetchMaterials(),
        fetchMaterialMovements(),
      ]);
      items.value = nextItems;
      movements.value = nextMovements;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入原料管理資料失敗";
    } finally {
      loading.value = false;
    }
  }

  async function createItem(payload: MaterialUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await createMaterial(payload);
      await loadMaterials();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立原料失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function updateItem(materialId: string, payload: MaterialUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await updateMaterial(materialId, payload);
      await loadMaterials();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新原料失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deactivateItem(materialId: string) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await deactivateMaterial(materialId);
      await loadMaterials();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "停用原料失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function submitMovement(materialId: string, payload: MaterialMovementRequest) {
    saving.value = true;
    errorMessage.value = "";
    try {
      await createMaterialMovement(materialId, payload);
      await loadMaterials();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立原料異動失敗";
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
    loadMaterials,
    loading,
    movements,
    saving,
    submitMovement,
    updateItem,
  };
});
