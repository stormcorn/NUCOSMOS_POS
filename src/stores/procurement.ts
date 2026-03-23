import { ref } from "vue";
import { defineStore } from "pinia";

import { ApiError } from "@/api/http";
import {
  createPurchaseOrder,
  createSupplier,
  deactivateSupplier,
  fetchPurchaseOrders,
  fetchReplenishmentSuggestions,
  fetchSuppliers,
  receivePurchaseOrder,
  updateSupplier,
} from "@/api/procurement";
import type {
  PurchaseOrder,
  PurchaseOrderCreateRequest,
  ReplenishmentSuggestion,
  SupplierItem,
  SupplierUpsertRequest,
} from "@/types/procurement";

export const useProcurementStore = defineStore("procurement", () => {
  const suppliers = ref<SupplierItem[]>([]);
  const purchaseOrders = ref<PurchaseOrder[]>([]);
  const replenishmentSuggestions = ref<ReplenishmentSuggestion[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");

  async function loadSuppliers() {
    loading.value = true;
    errorMessage.value = "";

    try {
      suppliers.value = await fetchSuppliers();
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入供應商資料失敗。";
    } finally {
      loading.value = false;
    }
  }

  async function loadPurchaseOrders() {
    loading.value = true;
    errorMessage.value = "";

    try {
      purchaseOrders.value = await fetchPurchaseOrders();
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入採購單資料失敗。";
    } finally {
      loading.value = false;
    }
  }

  async function loadReplenishmentSuggestions() {
    loading.value = true;
    errorMessage.value = "";

    try {
      replenishmentSuggestions.value = await fetchReplenishmentSuggestions();
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入補貨建議失敗。";
    } finally {
      loading.value = false;
    }
  }

  async function createSupplierRecord(payload: SupplierUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await createSupplier(payload);
      await loadSuppliers();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立供應商失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function updateSupplierRecord(supplierId: string, payload: SupplierUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await updateSupplier(supplierId, payload);
      await loadSuppliers();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新供應商失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deactivateSupplierRecord(supplierId: string) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await deactivateSupplier(supplierId);
      await loadSuppliers();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "停用供應商失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function createPurchaseOrderRecord(payload: PurchaseOrderCreateRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await createPurchaseOrder(payload);
      await Promise.all([loadPurchaseOrders(), loadReplenishmentSuggestions()]);
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立採購單失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function receivePurchaseOrderRecord(purchaseOrderId: string, note?: string) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await receivePurchaseOrder(purchaseOrderId, note);
      await Promise.all([loadPurchaseOrders(), loadReplenishmentSuggestions()]);
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "採購單收貨失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    createPurchaseOrderRecord,
    createSupplierRecord,
    deactivateSupplierRecord,
    errorMessage,
    loadPurchaseOrders,
    loadReplenishmentSuggestions,
    loadSuppliers,
    loading,
    purchaseOrders,
    receivePurchaseOrderRecord,
    replenishmentSuggestions,
    saving,
    suppliers,
    updateSupplierRecord,
  };
});
