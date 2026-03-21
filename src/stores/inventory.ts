import { ref } from "vue";
import { defineStore } from "pinia";

import { ApiError } from "@/api/http";
import {
  createInventoryMovement,
  fetchInventoryMovements,
  fetchInventoryStocks,
  updateInventoryReorderLevel,
} from "@/api/inventory";
import type { InventoryMovementRequest, InventoryMovementItem, InventoryStockItem } from "@/types/inventory";

export const useInventoryStore = defineStore("inventory", () => {
  const stocks = ref<InventoryStockItem[]>([]);
  const movements = ref<InventoryMovementItem[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");
  const lowStockOnly = ref(false);

  async function loadInventory() {
    loading.value = true;
    errorMessage.value = "";

    try {
      const [nextStocks, nextMovements] = await Promise.all([
        fetchInventoryStocks(lowStockOnly.value),
        fetchInventoryMovements(),
      ]);
      stocks.value = nextStocks;
      movements.value = nextMovements;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入庫存資料失敗。";
    } finally {
      loading.value = false;
    }
  }

  async function setLowStockFilter(nextValue: boolean) {
    lowStockOnly.value = nextValue;
    await loadInventory();
  }

  async function submitMovement(payload: InventoryMovementRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await createInventoryMovement(payload);
      await loadInventory();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "新增庫存異動失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function saveReorderLevel(productId: string, reorderLevel: number) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await updateInventoryReorderLevel(productId, reorderLevel);
      await loadInventory();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新補貨門檻失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    errorMessage,
    loadInventory,
    loading,
    lowStockOnly,
    movements,
    saveReorderLevel,
    saving,
    setLowStockFilter,
    stocks,
    submitMovement,
  };
});
