export type ProductMaterialComponent = {
  materialItemId: string;
  sku: string;
  name: string;
  unit: string;
  quantity: number;
  latestUnitCost: number | null;
  lineCost: number;
};

export type ProductManufacturedComponent = {
  manufacturedItemId: string;
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
  manufacturedComponentCount: number;
  materialComponentCount: number;
  packagingComponentCount: number;
  manufacturedCost: number;
  materialCost: number;
  packagingCost: number;
  totalCost: number;
};

export type ProductCustomizationOption = {
  id: string;
  name: string;
  priceDelta: number;
  defaultSelected: boolean;
  displayOrder: number;
  active: boolean;
};

export type ProductCustomizationGroup = {
  id: string;
  name: string;
  selectionMode: "SINGLE" | "MULTIPLE";
  required: boolean;
  minSelections: number;
  maxSelections: number;
  displayOrder: number;
  active: boolean;
  options: ProductCustomizationOption[];
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
  manufacturedComponents: ProductManufacturedComponent[];
  materialComponents: ProductMaterialComponent[];
  packagingComponents: ProductPackagingComponent[];
  customizationGroups: ProductCustomizationGroup[];
  recipeVersions: ProductRecipeVersion[];
  manufacturedCost: number;
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
  manufacturedComponents: Array<{
    manufacturedItemId: string;
    quantity: number;
  }>;
  materialComponents: Array<{
    materialItemId: string;
    quantity: number;
  }>;
  packagingComponents: Array<{
    packagingItemId: string;
    quantity: number;
  }>;
  customizationGroups: Array<{
    name: string;
    selectionMode: "SINGLE" | "MULTIPLE";
    required: boolean;
    minSelections: number;
    maxSelections: number;
    displayOrder: number;
    options: Array<{
      name: string;
      priceDelta: number;
      defaultSelected: boolean;
      displayOrder: number;
    }>;
  }>;
};
