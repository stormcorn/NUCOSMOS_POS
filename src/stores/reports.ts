import { ref } from "vue";
import { defineStore } from "pinia";

import { fetchSalesSummary } from "@/api/reports";
import { ApiError } from "@/api/http";
import type { SalesSummary } from "@/types/report";

function getTodayRange() {
  const now = new Date();
  const start = new Date(now);
  start.setHours(0, 0, 0, 0);

  const end = new Date(now);
  end.setHours(23, 59, 59, 999);

  return {
    from: start.toISOString(),
    to: end.toISOString(),
  };
}

export const useReportStore = defineStore("reports", () => {
  const salesSummary = ref<SalesSummary | null>(null);
  const loading = ref(false);
  const errorMessage = ref("");

  async function loadSalesSummary(from?: string, to?: string) {
    loading.value = true;
    errorMessage.value = "";

    try {
      const range = getTodayRange();
      salesSummary.value = await fetchSalesSummary(from ?? range.from, to ?? range.to);
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法取得營收摘要";
    } finally {
      loading.value = false;
    }
  }

  return {
    errorMessage,
    loadSalesSummary,
    loading,
    salesSummary,
  };
});
