export type SupplyMovementType =
  | "PURCHASE_IN"
  | "ADJUSTMENT_IN"
  | "ADJUSTMENT_OUT"
  | "DAMAGE_OUT"
  | "CONSUME_OUT"
  | "RETURN_IN";

export type MaterialAdminItem = {
  id: string;
  sku: string;
  name: string;
  unit: string;
  purchaseUnit: string;
  purchaseToStockRatio: number;
  imageUrl: string | null;
  description: string | null;
  quantityOnHand: number;
  reorderLevel: number;
  latestUnitCost: number | null;
  latestPurchaseUnitCost: number | null;
  lowStock: boolean;
  active: boolean;
};

export type MaterialLotItem = {
  id: string;
  materialId: string;
  sku: string;
  materialName: string;
  unit: string;
  batchCode: string | null;
  expiryDate: string | null;
  manufacturedAt: string | null;
  receivedQuantity: number;
  remainingQuantity: number;
  unitCost: number | null;
  sourceType: string;
  sourceId: string | null;
  receivedAt: string;
};

export type MaterialMovementItem = {
  id: string;
  materialId: string;
  sku: string;
  materialName: string;
  unit: string;
  movementType: SupplyMovementType;
  quantity: number;
  quantityDelta: number;
  quantityAfter: number;
  unitCost: number | null;
  note: string | null;
  occurredAt: string;
};

export type MaterialUpsertRequest = {
  sku: string;
  name: string;
  unit: string;
  purchaseUnit: string;
  purchaseToStockRatio: number;
  imageUrl: string;
  description: string;
  reorderLevel: number;
  latestUnitCost?: number | null;
};

export type MaterialMovementRequest = {
  movementType: SupplyMovementType;
  quantity: number;
  unitCost?: number | null;
  batchCode?: string;
  expiryDate?: string | null;
  manufacturedAt?: string | null;
  note?: string;
};
