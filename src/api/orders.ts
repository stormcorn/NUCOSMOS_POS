import { apiRequest } from "@/api/http";
import type { PagedResponse } from "@/types/api";
import type { OrderDetail, OrderSummary } from "@/types/order";

export type OrderListQuery = {
  status?: string;
  paymentStatus?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDirection?: "asc" | "desc";
};

export function fetchOrders(query: OrderListQuery) {
  return apiRequest<PagedResponse<OrderSummary>>("/api/v1/orders", { query });
}

export function fetchOrderDetail(orderId: string) {
  return apiRequest<OrderDetail>(`/api/v1/orders/${orderId}`);
}
