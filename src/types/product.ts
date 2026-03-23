export type ProductMaterialComponent = {
  materialItemId: string;
  sku: string;
  name: string;
  unit: string;
  quantity: number;
  latestUnitCost: number | null;
  lineCost: number;
};

export type ProductPackagingComponent = {
  packagingItemId: string;
  sku: string;
  name: string;
  unit: string;
  specification: string | null;
  quantity: number;
  latestUnitCost: number | null;
  lineCost: number;
};

export type ProductRecipeVersion = {
  id: string | null;
  versionNumber: number;
  status: string;
  note: string | null;
  effectiveAt: string;
  materialComponentCount: number;
  packagingComponentCount: number;
  materialCost: number;
  packagingCost: number;
  totalCost: number;
};

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
  campaignEnabled: boolean;
  campaignActive: boolean;
  campaignLabel: string | null;
  campaignPrice: number | null;
  campaignStartsAt: string | null;
  campaignEndsAt: string | null;
  displayPrice: number;
  active: boolean;
  materialComponents: ProductMaterialComponent[];
  packagingComponents: ProductPackagingComponent[];
  recipeVersions: ProductRecipeVersion[];
  materialCost: number;
  packagingCost: number;
  totalCost: number;
};

export type ProductUpsertRequest = {
  categoryId: string;
  sku: string;
  name: string;
  description: string;
  imageUrl: string;
  price: number;
  campaignEnabled: boolean;
  campaignLabel?: string;
  campaignPrice?: number;
  campaignStartsAt?: string;
  campaignEndsAt?: string;
  recipeNote?: string;
  materialComponents: Array<{
    materialItemId: string;
    quantity: number;
  }>;
  packagingComponents: Array<{
    packagingItemId: string;
    quantity: number;
  }>;
};
