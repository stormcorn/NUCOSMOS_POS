export type StocktakeItemDraft = {
  productId: string;
  countedSellableQuantity: number;
  reasonCode?: string;
  note?: string;
};

export type StocktakeCreateRequest = {
  note?: string;
  items: StocktakeItemDraft[];
};

export type StocktakeItem = {
  id: string;
  productId: string;
  productSku: string;
  productName: string;
  categoryName: string;
  expectedSellableQuantity: number;
  countedSellableQuantity: number;
  varianceQuantity: number;
  reasonCode: string | null;
  note: string | null;
};

export type StocktakeRecord = {
  id: string;
  status: string;
  storeCode: string;
  createdByEmployeeCode: string;
  note: string | null;
  countedAt: string;
  postedAt: string;
  items: StocktakeItem[];
};
