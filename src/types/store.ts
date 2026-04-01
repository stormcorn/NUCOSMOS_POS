export type StoreSummary = {
  id: string;
  code: string;
  name: string;
  timezone: string;
  currencyCode: string;
  status: string;
  receiptFooterText: string;
};

export type StoreReceiptSettings = {
  storeId: string;
  storeCode: string;
  storeName: string;
  receiptFooterText: string;
};
