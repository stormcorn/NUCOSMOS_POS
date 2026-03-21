import { ref } from "vue";
import { defineStore } from "pinia";

import { closeShift, fetchCurrentShift, openShift } from "@/api/shifts";
import { ApiError } from "@/api/http";
import type { ShiftSummary } from "@/types/shift";

export const useShiftStore = defineStore("shifts", () => {
  const currentShift = ref<ShiftSummary | null>(null);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");

  async function loadCurrentShift() {
    loading.value = true;
    errorMessage.value = "";

    try {
      currentShift.value = await fetchCurrentShift();
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        currentShift.value = null;
        return;
      }

      errorMessage.value = error instanceof ApiError ? error.message : "無法取得班次資料";
    } finally {
      loading.value = false;
    }
  }

  async function submitOpenShift(openingCashAmount: number, note: string) {
    saving.value = true;
    errorMessage.value = "";

    try {
      currentShift.value = await openShift({ openingCashAmount, note });
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "開班失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function submitCloseShift(closingCashAmount: number, note: string) {
    if (!currentShift.value) {
      errorMessage.value = "目前沒有可關閉的班次";
      return false;
    }

    saving.value = true;
    errorMessage.value = "";

    try {
      currentShift.value = await closeShift(currentShift.value.id, { closingCashAmount, note });
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "關班失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    currentShift,
    errorMessage,
    loadCurrentShift,
    loading,
    saving,
    submitCloseShift,
    submitOpenShift,
  };
});
