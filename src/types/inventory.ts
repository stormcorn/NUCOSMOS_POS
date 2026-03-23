export type InventoryMovementType =
  | "PURCHASE_IN"
  | "SALE_OUT"
  | "REFUND_IN"
  | "REFUND_DEFECT"
  | "DEFECTIVE_RESTORE"
  | "ADJUSTMENT_IN"
  | "ADJUSTMENT_OUT"
  | "DAMAGE_OUT"
  | "SCRAP_OUT"
  | "SAMPLE_OUT"
  | "PRODUCTION_CONSUME";

export type InventoryStockItem = {
  productId: string;
  sku: string;
  name: string;
  categoryName: string;
  imageUrl: string | null;
  sellableQuantity: number;
  defectiveQuantity: number;
  quantityOnHand: number;
  reorderLevel: number;
  lowStock: boolean;
};

export type InventoryMovementItem = {
  id: string;
  productId: string;
  sku: string;
  productName: string;
  movementType: InventoryMovementType;
  stockBucket: "SELLABLE" | "DEFECTIVE";
  quantity: number;
  quantityDelta: number;
  quantityAfter: number;
  sellableQuantityDelta: number;
  defectiveQuantityDelta: number;
  sellableQuantityAfter: number;
  defectiveQuantityAfter: number;
  unitCost: number | null;
  reasonCode: string | null;
  note: string | null;
  referenceType: string | null;
  referenceId: string | null;
  occurredAt: string;
};

export type InventoryMovementRequest = {
  productId: string;
  movementType: InventoryMovementType;
  quantity: number;
  unitCost?: number | null;
  reasonCode?: string;
  note?: string;
};

export type DefectiveInventoryActionRequest = {
  quantity: number;
  reasonCode: string;
  note?: string;
};
