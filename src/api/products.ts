import { apiRequest } from "@/api/http";
import type { ProductAdminItem, ProductCategory, ProductUpsertRequest } from "@/types/product";

export function fetchProductCategories() {
  return apiRequest<ProductCategory[]>("/api/v1/admin/product-categories");
}

export function fetchProducts(active?: boolean) {
  return apiRequest<ProductAdminItem[]>("/api/v1/admin/products", {
    query: active === undefined ? {} : { active },
  });
}

export function createProduct(payload: ProductUpsertRequest) {
  return apiRequest<ProductAdminItem>("/api/v1/admin/products", {
    method: "POST",
    body: payload,
  });
}

export function updateProduct(productId: string, payload: ProductUpsertRequest) {
  return apiRequest<ProductAdminItem>(`/api/v1/admin/products/${productId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivateProduct(productId: string) {
  return apiRequest<ProductAdminItem>(`/api/v1/admin/products/${productId}/deactivate`, {
    method: "POST",
  });
}
