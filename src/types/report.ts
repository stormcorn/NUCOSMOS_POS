export type SalesSummary = {
  storeCode: string;
  from: string;
  to: string;
  orderCount: number;
  voidedOrderCount: number;
  grossSalesAmount: number;
  refundedAmount: number;
  netSalesAmount: number;
  cashSalesAmount: number;
  cardSalesAmount: number;
  averageOrderAmount: number;
  cogsAmount: number;
  refundedCogsAmount: number;
  netCogsAmount: number;
  grossProfitAmount: number;
  grossMarginRate: number;
};

export type SalesTrendPoint = {
  bucketLabel: string;
  bucketStart: string;
  orderCount: number;
  grossSalesAmount: number;
  refundedAmount: number;
  netSalesAmount: number;
};

export type SalesTrend = {
  storeCode: string;
  from: string;
  to: string;
  granularity: string;
  points: SalesTrendPoint[];
};

export type InventoryKpiSummary = {
  productSkuCount: number;
  productLowStockCount: number;
  totalSellableQuantity: number;
  totalDefectiveQuantity: number;
  materialSkuCount: number;
  materialLowStockCount: number;
  totalMaterialQuantity: number;
  packagingSkuCount: number;
  packagingLowStockCount: number;
  totalPackagingQuantity: number;
};

export type LowStockSnapshot = {
  itemType: string;
  sku: string;
  name: string;
  secondaryLabel: string | null;
  unit: string;
  quantityOnHand: number;
  reorderLevel: number;
};

export type MovementTotal = {
  scope: string;
  movementType: string;
  entryCount: number;
  totalQuantity: number;
  netDelta: number;
};

export type ConsumptionSummary = {
  scope: string;
  sku: string;
  name: string;
  unit: string;
  consumedQuantity: number;
  consumedCost: number;
};

export type DefectiveWasteSummary = {
  sku: string;
  name: string;
  movementType: string;
  affectedQuantity: number;
};

export type ExpiringLotSnapshot = {
  scope: string;
  sku: string;
  name: string;
  batchCode: string | null;
  expiryDate: string;
  remainingQuantity: number;
  unit: string;
  daysUntilExpiry: number;
};

export type InventoryAnalytics = {
  storeCode: string;
  from: string;
  to: string;
  summary: InventoryKpiSummary;
  lowStockProducts: LowStockSnapshot[];
  lowStockMaterials: LowStockSnapshot[];
  lowStockPackaging: LowStockSnapshot[];
  productMovementTotals: MovementTotal[];
  materialMovementTotals: MovementTotal[];
  packagingMovementTotals: MovementTotal[];
  materialConsumption: ConsumptionSummary[];
  packagingConsumption: ConsumptionSummary[];
  defectiveAndWaste: DefectiveWasteSummary[];
  expiringMaterialLots: ExpiringLotSnapshot[];
  expiringPackagingLots: ExpiringLotSnapshot[];
};
