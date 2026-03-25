export type { SupplyMovementType } from "@/types/materials";
import type { SupplyMovementType } from "@/types/materials";

export type ManufacturedAdminItem = {
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

export type ManufacturedLotItem = {
  id: string;
  manufacturedItemId: string;
  sku: string;
  manufacturedName: string;
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

export type ManufacturedMovementItem = {
  id: string;
  manufacturedItemId: string;
  sku: string;
  manufacturedName: string;
  unit: string;
  movementType: SupplyMovementType;
  quantity: number;
  quantityDelta: number;
  quantityAfter: number;
  unitCost: number | null;
  note: string | null;
  occurredAt: string;
};

export type ManufacturedUpsertRequest = {
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

export type ManufacturedMovementRequest = {
  movementType: SupplyMovementType;
  quantity: number;
  unitCost?: number | null;
  batchCode?: string;
  expiryDate?: string | null;
  manufacturedAt?: string | null;
  note?: string;
};
