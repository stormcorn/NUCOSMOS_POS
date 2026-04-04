import { computed, ref } from "vue";
import { defineStore } from "pinia";

import { bulkDeleteTestOrders, fetchOrderDetail, fetchOrders } from "@/api/orders";
import { ApiError } from "@/api/http";
import type { BulkDeleteTestOrdersResult, OrderDetail, OrderSummary } from "@/types/order";

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
  const actionMessage = ref("");
  const bulkDeleting = ref(false);

  const statusFilter = ref("");
  const paymentStatusFilter = ref("");
  const fromFilter = ref("");
  const toFilter = ref("");

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
        from: normalizeDateTimeFilter(fromFilter.value),
        to: normalizeDateTimeFilter(toFilter.value),
      });

      items.value = response.items;
      totalElements.value = response.totalElements;
      totalPages.value = response.totalPages;
      hasNext.value = response.hasNext;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法載入訂單列表";
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
      detailErrorMessage.value = error instanceof ApiError ? error.message : "無法載入訂單明細";
    } finally {
      detailLoading.value = false;
    }
  }

  function clearDetail() {
    detail.value = null;
    detailErrorMessage.value = "";
  }

  async function deleteTestOrdersInRange() {
    if (!fromFilter.value || !toFilter.value) {
      throw new ApiError(400, "請先選擇開始與結束時間");
    }

    bulkDeleting.value = true;
    actionMessage.value = "";
    errorMessage.value = "";

    try {
      const result = await bulkDeleteTestOrders(
        normalizeDateTimeFilter(fromFilter.value) ?? fromFilter.value,
        normalizeDateTimeFilter(toFilter.value) ?? toFilter.value,
      );
      actionMessage.value = buildBulkDeleteMessage(result);
      await loadOrders();
      if (detail.value?.testOrder) {
        detail.value = null;
      }
      return result;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法刪除測試訂單";
      throw error;
    } finally {
      bulkDeleting.value = false;
    }
  }

  function buildBulkDeleteMessage(result: BulkDeleteTestOrdersResult) {
    const restored = result.inventoryRestoredCount > 0 ? `，回補庫存 ${result.inventoryRestoredCount} 筆` : "";
    const skipped = result.skippedCount > 0
      ? `，略過 ${result.skippedCount} 筆：${result.skippedOrderNumbers.join("、")}`
      : "";
    return `已刪除 ${result.deletedCount} / ${result.matchedCount} 筆測試訂單${restored}${skipped}`;
  }

  function normalizeDateTimeFilter(value: string) {
    if (!value) {
      return undefined;
    }

    return new Date(value).toISOString();
  }

  return {
    actionMessage,
    bulkDeleting,
    clearDetail,
    detail,
    detailErrorMessage,
    deleteTestOrdersInRange,
    detailLoading,
    errorMessage,
    fromFilter,
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
    toFilter,
    totalElements,
    totalPages,
  };
});
