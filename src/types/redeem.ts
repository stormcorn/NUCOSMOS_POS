export type ReceiptRedeemTicket = {
  token: string;
  claimCode: string;
  redeemUrl: string;
  orderNumber: string;
  storeCode: string;
  storeName: string;
  itemCount: number;
  totalAmount: number;
  paymentStatus: string;
  orderedAt: string;
  claimedAt: string | null;
  eligible: boolean;
  claimed: boolean;
  claimable: boolean;
  message: string;
};
