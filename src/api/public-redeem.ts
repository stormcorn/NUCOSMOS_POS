import { apiRequest } from "@/api/http";
import type { ReceiptRedeemTicket } from "@/types/redeem";

export function fetchRedeemTicketByToken(token: string) {
  return apiRequest<ReceiptRedeemTicket>(`/api/v1/public/redeem/${encodeURIComponent(token)}`, {
    method: "GET",
    auth: false,
  });
}

export function fetchRedeemTicketByCode(code: string) {
  return apiRequest<ReceiptRedeemTicket>("/api/v1/public/redeem/search", {
    method: "GET",
    query: { code },
    auth: false,
  });
}

export function claimRedeemTicket(token: string) {
  return apiRequest<ReceiptRedeemTicket>(`/api/v1/public/redeem/${encodeURIComponent(token)}/claim`, {
    method: "POST",
    auth: false,
  });
}
