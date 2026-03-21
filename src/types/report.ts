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
};
