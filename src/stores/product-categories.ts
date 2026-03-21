import { ref } from "vue";
import { defineStore } from "pinia";

import { ApiError } from "@/api/http";
import {
  createProductCategory as createProductCategoryRequest,
  deactivateProductCategory as deactivateProductCategoryRequest,
  fetchProductCategories,
  updateProductCategory as updateProductCategoryRequest,
} from "@/api/product-categories";
import type { ProductCategory, ProductCategoryUpsertRequest } from "@/types/product";

export const useProductCategoryStore = defineStore("product-categories", () => {
  const categories = ref<ProductCategory[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");

  async function loadCategories() {
    loading.value = true;
    errorMessage.value = "";

    try {
      categories.value = await fetchProductCategories();
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "載入商品分類失敗。";
    } finally {
      loading.value = false;
    }
  }

  async function createCategory(payload: ProductCategoryUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await createProductCategoryRequest(payload);
      await loadCategories();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "建立商品分類失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function updateCategory(categoryId: string, payload: ProductCategoryUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await updateProductCategoryRequest(categoryId, payload);
      await loadCategories();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新商品分類失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deactivateCategory(categoryId: string) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await deactivateProductCategoryRequest(categoryId);
      await loadCategories();
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "停用商品分類失敗。";
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    categories,
    createCategory,
    deactivateCategory,
    errorMessage,
    loadCategories,
    loading,
    saving,
    updateCategory,
  };
});
