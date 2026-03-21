export type OrderSummary = {
  id: string;
  orderNumber: string;
  status: string;
  paymentStatus: string;
  storeCode: string;
  deviceCode: string | null;
  createdByEmployeeCode: string;
  itemCount: number;
  totalAmount: number;
  paidAmount: number;
  refundedAmount: number;
  orderedAt: string;
  closedAt: string | null;
};

export type OrderItemDetail = {
  id: string;
  lineNumber: number;
  productId: string;
  productSku: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotalAmount: number;
  note: string | null;
};

export type PaymentDetail = {
  id: string;
  paymentMethod: string;
  status: string;
  amount: number;
  amountReceived: number | null;
  changeAmount: number | null;
  cardTerminalProvider: string | null;
  cardTransactionStatus: string | null;
  cardTerminalTransactionId: string | null;
  cardApprovalCode: string | null;
  cardMaskedPan: string | null;
  cardBatchNumber: string | null;
  cardRetrievalReferenceNumber: string | null;
  cardEntryMode: string | null;
  authorizedAt: string | null;
  capturedAt: string | null;
  voidedAt: string | null;
  refundedAt: string | null;
  createdByEmployeeCode: string;
  note: string | null;
  paidAt: string | null;
};

export type RefundDetail = {
  id: string;
  paymentId: string | null;
  refundMethod: string;
  amount: number;
  reason: string | null;
  status: string;
  createdByEmployeeCode: string;
  refundedAt: string | null;
};

export type OrderDetail = {
  id: string;
  orderNumber: string;
  status: string;
  paymentStatus: string;
  storeCode: string;
  deviceCode: string | null;
  createdByEmployeeCode: string;
  itemCount: number;
  subtotalAmount: number;
  totalAmount: number;
  paidAmount: number;
  changeAmount: number;
  refundedAmount: number;
  note: string | null;
  orderedAt: string;
  closedAt: string | null;
  voidedAt: string | null;
  voidNote: string | null;
  items: OrderItemDetail[];
  payments: PaymentDetail[];
  refunds: RefundDetail[];
};
