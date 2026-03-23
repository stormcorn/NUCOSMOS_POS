import { apiRequest } from "@/api/http";
import type { InventoryAnalytics, ProfitabilityAnalytics, SalesSummary, SalesTrend } from "@/types/report";

export function fetchSalesSummary(from: string, to: string) {
  return apiRequest<SalesSummary>("/api/v1/reports/sales-summary", {
    query: { from, to },
  });
}

export function fetchInventoryAnalytics(from: string, to: string) {
  return apiRequest<InventoryAnalytics>("/api/v1/reports/inventory-analytics", {
    query: { from, to },
  });
}

export function fetchSalesTrend(from: string, to: string) {
  return apiRequest<SalesTrend>("/api/v1/reports/sales-trend", {
    query: { from, to },
  });
}

export function fetchProfitabilityAnalysis(from: string, to: string) {
  return apiRequest<ProfitabilityAnalytics>("/api/v1/reports/profitability-analysis", {
    query: { from, to },
  });
}
