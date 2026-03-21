import { computed, ref } from "vue";
import { defineStore } from "pinia";

import {
  createProduct as createProductRequest,
  deactivateProduct as deactivateProductRequest,
  fetchProductCategories,
  fetchProducts,
  updateProduct as updateProductRequest,
} from "@/api/products";
import { ApiError } from "@/api/http";
import type { ProductAdminItem, ProductCategory, ProductUpsertRequest } from "@/types/product";

export const useProductStore = defineStore("products", () => {
  const categories = ref<ProductCategory[]>([]);
  const products = ref<ProductAdminItem[]>([]);
  const loading = ref(false);
  const saving = ref(false);
  const errorMessage = ref("");
  const activeFilter = ref<"all" | "active" | "inactive">("all");

  const categoryMix = computed(() => {
    const counts = new Map<string, number>();

    for (const product of products.value) {
      counts.set(product.categoryName, (counts.get(product.categoryName) ?? 0) + 1);
    }

    const total = products.value.length || 1;
    return Array.from(counts.entries()).map(([label, count]) => ({
      label,
      value: Math.round((count / total) * 100),
    }));
  });

  async function loadCategories() {
    categories.value = await fetchProductCategories();
  }

  async function loadProducts(filter = activeFilter.value) {
    loading.value = true;
    errorMessage.value = "";
    activeFilter.value = filter;

    try {
      const active = filter === "all" ? undefined : filter === "active";
      products.value = await fetchProducts(active);
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法取得商品資料";
    } finally {
      loading.value = false;
    }
  }

  async function loadCatalog(filter = activeFilter.value) {
    loading.value = true;
    errorMessage.value = "";
    activeFilter.value = filter;

    try {
      const active = filter === "all" ? undefined : filter === "active";
      const [nextCategories, nextProducts] = await Promise.all([
        fetchProductCategories(),
        fetchProducts(active),
      ]);
      categories.value = nextCategories;
      products.value = nextProducts;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "無法取得商品資料";
    } finally {
      loading.value = false;
    }
  }

  async function createProduct(payload: ProductUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await createProductRequest(payload);
      await loadProducts(activeFilter.value);
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "新增商品失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function updateProduct(productId: string, payload: ProductUpsertRequest) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await updateProductRequest(productId, payload);
      await loadProducts(activeFilter.value);
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "更新商品失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function deactivateProduct(productId: string) {
    saving.value = true;
    errorMessage.value = "";

    try {
      await deactivateProductRequest(productId);
      await loadProducts(activeFilter.value);
      return true;
    } catch (error) {
      errorMessage.value = error instanceof ApiError ? error.message : "停用商品失敗";
      return false;
    } finally {
      saving.value = false;
    }
  }

  return {
    activeFilter,
    categories,
    categoryMix,
    createProduct,
    deactivateProduct,
    errorMessage,
    loadCatalog,
    loadCategories,
    loadProducts,
    loading,
    products,
    saving,
    updateProduct,
  };
});
