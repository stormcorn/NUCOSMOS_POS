import { apiRequest } from "@/api/http";
import type { ShiftCloseRequest, ShiftOpenRequest, ShiftSummary } from "@/types/shift";

export function fetchCurrentShift() {
  return apiRequest<ShiftSummary>("/api/v1/shifts/current");
}

export function openShift(payload: ShiftOpenRequest) {
  return apiRequest<ShiftSummary>("/api/v1/shifts/open", {
    method: "POST",
    body: payload,
  });
}

export function closeShift(shiftId: string, payload: ShiftCloseRequest) {
  return apiRequest<ShiftSummary>(`/api/v1/shifts/${shiftId}/close`, {
    method: "POST",
    body: payload,
  });
}
