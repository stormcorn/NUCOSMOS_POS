import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { fetchOrderDetail, fetchOrders } from "@/api/orders";
import { ApiError } from "@/api/http";
import type { OrderDetail, OrderSummary } from "@/types/order";

export const useOrderStore = defineStore("orders", () => {
  const items = ref<OrderSummary[]>([]);
  const loading = ref(false);
  const errorMessage = ref("");
  const page = ref(0);
  const size = ref(20);
  const totalElements = ref(0);
  const totalPages = ref(0);
  const hasNext = ref(false);
  const detail = ref<OrderDetail | null>(null);
  const detailLoading = ref(false);
  const detailErrorMessage = ref("");

  const statusFilter = ref("");
  const paymentStatusFilter = ref("");

  const recentOrders = computed(() => items.value.slice(0, 5));

  async function loadOrders() {
    loading.value = true;
    errorMessage.value = "";

    try {
      const response = await fetchOrders({
        page: page.value,
        size: size.value,
        status: statusFilter.value || undefined,
        paymentStatus: paymentStatusFilter.value || undefined,
      });

      items.value = response.items;
      totalElements.value = response.totalElements;
      totalPages.value = response.totalPages;
      hasNext.value = response.hasNext;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法取得訂單資料";
    } finally {
      loading.value = false;
    }
  }

  function setPage(nextPage: number) {
    page.value = nextPage;
  }

  async function loadOrderDetail(orderId: string) {
    detailLoading.value = true;
    detailErrorMessage.value = "";

    try {
      detail.value = await fetchOrderDetail(orderId);
    } catch (error) {
      detailErrorMessage.value = error instanceof ApiError ? error.message : "無法取得訂單詳情";
    } finally {
      detailLoading.value = false;
    }
  }

  function clearDetail() {
    detail.value = null;
    detailErrorMessage.value = "";
  }

  return {
    clearDetail,
    detail,
    detailErrorMessage,
    detailLoading,
    errorMessage,
    hasNext,
    items,
    loadOrderDetail,
    loadOrders,
    loading,
    page,
    paymentStatusFilter,
    recentOrders,
    setPage,
    size,
    statusFilter,
    totalElements,
    totalPages,
  };
});
