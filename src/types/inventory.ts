export type InventoryStockItem = {
  productId: string;
  sku: string;
  name: string;
  categoryName: string;
  imageUrl: string | null;
  quantityOnHand: number;
  reorderLevel: number;
  lowStock: boolean;
};

export type InventoryMovementItem = {
  id: string;
  productId: string;
  sku: string;
  productName: string;
  movementType: string;
  quantity: number;
  quantityDelta: number;
  quantityAfter: number;
  unitCost: number | null;
  note: string | null;
  referenceType: string | null;
  referenceId: string | null;
  occurredAt: string;
};

export type InventoryMovementRequest = {
  productId: string;
  movementType: string;
  quantity: number;
  unitCost?: number | null;
  note?: string;
};
