import { apiRequest } from "@/api/http";
import type { SalesSummary } from "@/types/report";

export function fetchSalesSummary(from: string, to: string) {
  return apiRequest<SalesSummary>("/api/v1/reports/sales-summary", {
    query: { from, to },
  });
}
