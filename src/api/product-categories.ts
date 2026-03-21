import { apiRequest } from "@/api/http";
import type { ProductCategory, ProductCategoryUpsertRequest } from "@/types/product";

export function fetchProductCategories() {
  return apiRequest<ProductCategory[]>("/api/v1/admin/product-categories");
}

export function createProductCategory(payload: ProductCategoryUpsertRequest) {
  return apiRequest<ProductCategory>("/api/v1/admin/product-categories", {
    method: "POST",
    body: payload,
  });
}

export function updateProductCategory(categoryId: string, payload: ProductCategoryUpsertRequest) {
  return apiRequest<ProductCategory>(`/api/v1/admin/product-categories/${categoryId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivateProductCategory(categoryId: string) {
  return apiRequest<ProductCategory>(`/api/v1/admin/product-categories/${categoryId}/deactivate`, {
    method: "POST",
  });
}
