export type ShiftSummary = {
  id: string;
  status: string;
  storeCode: string;
  deviceCode: string;
  openedByEmployeeCode: string;
  closedByEmployeeCode: string | null;
  openingCashAmount: number;
  closingCashAmount: number | null;
  expectedCashAmount: number | null;
  cashSalesAmount: number | null;
  cardSalesAmount: number | null;
  refundedAmount: number | null;
  netSalesAmount: number | null;
  orderCount: number | null;
  voidedOrderCount: number | null;
  note: string | null;
  closeNote: string | null;
  openedAt: string;
  closedAt: string | null;
};

export type ShiftOpenRequest = {
  openingCashAmount: number;
  note: string;
};

export type ShiftCloseRequest = {
  closingCashAmount: number;
  note: string;
};
