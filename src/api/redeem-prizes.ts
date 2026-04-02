import { apiRequest } from "@/api/http";

export type RedeemPrizeAdminItem = {
  id: string;
  name: string;
  description: string | null;
  probabilityPercent: number;
  remainingQuantity: number;
  active: boolean;
  displayOrder: number;
};

export type RedeemPrizeUpsertPayload = {
  name: string;
  description?: string;
  probabilityPercent: number;
  remainingQuantity: number;
  active: boolean;
  displayOrder: number;
};

export function fetchRedeemPrizes() {
  return apiRequest<RedeemPrizeAdminItem[]>("/api/v1/admin/redeem-prizes");
}

export function createRedeemPrize(payload: RedeemPrizeUpsertPayload) {
  return apiRequest<RedeemPrizeAdminItem>("/api/v1/admin/redeem-prizes", {
    method: "POST",
    body: payload,
  });
}

export function updateRedeemPrize(prizeId: string, payload: RedeemPrizeUpsertPayload) {
  return apiRequest<RedeemPrizeAdminItem>(`/api/v1/admin/redeem-prizes/${prizeId}`, {
    method: "PUT",
    body: payload,
  });
}

export function deactivateRedeemPrize(prizeId: string) {
  return apiRequest<RedeemPrizeAdminItem>(`/api/v1/admin/redeem-prizes/${prizeId}/deactivate`, {
    method: "POST",
  });
}
