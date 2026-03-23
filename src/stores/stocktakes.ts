import { ref } from "vue";
import { defineStore } from "pinia";

import { createInventoryStocktake, fetchInventoryStocktakes } from "@/api/stocktakes";
import { ApiError } from "@/api/http";
import type { StocktakeCreateRequest, StocktakeRecord } from "@/types/stocktake";

export const useStocktakeStore = defineStore("stocktakes", () => {
  const items = ref<StocktakeRecord[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");

  async function loadStocktakes() {
    loading.value = true;
    errorMessage.value = "";

    try {
      items.value = await fetchInventoryStocktakes();
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法載入盤點單資料";
    } finally {
      loading.value = false;
    }
  }

  async function submitStocktake(payload: StocktakeCreateRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      const stocktake = await createInventoryStocktake(payload);
      items.value = [stocktake, ...items.value].slice(0, 20);
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立盤點單失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    errorMessage,
    items,
    loadStocktakes,
    loading,
    saving,
    submitStocktake,
  };
});
