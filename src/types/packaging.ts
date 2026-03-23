export type { SupplyMovementType } from "@/types/materials";
import type { SupplyMovementType } from "@/types/materials";

export type PackagingAdminItem = {
  id: string;
  sku: string;
  name: string;
  unit: string;
  specification: string | null;
  description: string | null;
  quantityOnHand: number;
  reorderLevel: number;
  latestUnitCost: number | null;
  lowStock: boolean;
  active: boolean;
};

export type PackagingMovementItem = {
  id: string;
  packagingItemId: string;
  sku: string;
  packagingName: string;
  unit: string;
  movementType: SupplyMovementType;
  quantity: number;
  quantityDelta: number;
  quantityAfter: number;
  unitCost: number | null;
  note: string | null;
  occurredAt: string;
};

export type PackagingUpsertRequest = {
  sku: string;
  name: string;
  unit: string;
  specification: string;
  description: string;
  reorderLevel: number;
  latestUnitCost?: number | null;
};

export type PackagingMovementRequest = {
  movementType: SupplyMovementType;
  quantity: number;
  unitCost?: number | null;
  note?: string;
};
