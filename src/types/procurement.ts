export type SupplierItem = {
  id: string;
  code: string;
  name: string;
  contactName: string | null;
  phone: string | null;
  email: string | null;
  note: string | null;
  active: boolean;
};

export type SupplierUpsertRequest = {
  code: string;
  name: string;
  contactName?: string;
  phone?: string;
  email?: string;
  note?: string;
};

export type ReplenishmentSuggestion = {
  itemType: "MATERIAL" | "MANUFACTURED" | "PACKAGING";
  itemId: string;
  sku: string;
  name: string;
  stockUnit: string;
  purchaseUnit: string;
  purchaseToStockRatio: number;
  quantityOnHand: number;
  reorderLevel: number;
  suggestedOrderQuantity: number;
  latestUnitCost: number | null;
  latestPurchaseUnitCost: number | null;
  estimatedOrderCost: number | null;
};

export type PurchaseOrderLine = {
  id: string;
  itemType: "MATERIAL" | "MANUFACTURED" | "PACKAGING";
  itemId: string;
  itemSku: string;
  itemName: string;
  unit: string;
  stockUnit: string;
  purchaseToStockRatio: number;
  orderedQuantity: number;
  receivedQuantity: number;
  receivedStockQuantity: number;
  unitCost: number | null;
  batchCode: string | null;
  expiryDate: string | null;
  manufacturedAt: string | null;
  note: string | null;
};

export type PurchaseOrder = {
  id: string;
  orderNumber: string;
  status: string;
  storeCode: string;
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  createdByEmployeeCode: string;
  note: string | null;
  expectedAt: string | null;
  receivedAt: string | null;
  lines: PurchaseOrderLine[];
};

export type PurchaseOrderLineRequest = {
  itemType: "MATERIAL" | "MANUFACTURED" | "PACKAGING";
  itemId: string;
  orderedQuantity: number;
  unitCost?: number | null;
  batchCode?: string;
  expiryDate?: string | null;
  manufacturedAt?: string | null;
  note?: string;
};

export type PurchaseOrderCreateRequest = {
  supplierId: string;
  note?: string;
  expectedAt?: string | null;
  lines: PurchaseOrderLineRequest[];
};
