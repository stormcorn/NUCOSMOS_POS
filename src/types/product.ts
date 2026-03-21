export type ProductCategory = {
  id: string;
  code: string;
  name: string;
  displayOrder: number;
  active: boolean;
};

export type ProductCategoryUpsertRequest = {
  code: string;
  name: string;
  displayOrder: number;
};

export type ProductAdminItem = {
  id: string;
  categoryId: string;
  categoryCode: string;
  categoryName: string;
  sku: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  price: number;
  active: boolean;
};

export type ProductUpsertRequest = {
  categoryId: string;
  sku: string;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
};
