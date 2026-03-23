import { ref } from "vue";
import { defineStore } from "pinia";

import { ApiError } from "@/api/http";
import { fetchInventoryAnalytics, fetchSalesSummary, fetchSalesTrend } from "@/api/reports";
import type { InventoryAnalytics, SalesSummary, SalesTrend } from "@/types/report";

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
  const salesTrend = ref<SalesTrend | null>(null);
  const inventoryAnalytics = ref<InventoryAnalytics | null>(null);
  const loading = ref(false);
  const errorMessage = ref("");

  async function loadAllReports(from?: string, to?: string) {
    loading.value = true;
    errorMessage.value = "";

    try {
      const range = getTodayRange();
      const finalFrom = from ?? range.from;
      const finalTo = to ?? range.to;

      const [nextSalesSummary, nextSalesTrend, nextInventoryAnalytics] = await Promise.all([
        fetchSalesSummary(finalFrom, finalTo),
        fetchSalesTrend(finalFrom, finalTo),
        fetchInventoryAnalytics(finalFrom, finalTo),
      ]);

      salesSummary.value = nextSalesSummary;
      salesTrend.value = nextSalesTrend;
      inventoryAnalytics.value = nextInventoryAnalytics;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入報表資料時發生錯誤。";
    } finally {
      loading.value = false;
    }
  }

  return {
    errorMessage,
    inventoryAnalytics,
    loadAllReports,
    loading,
    salesSummary,
    salesTrend,
  };
});
