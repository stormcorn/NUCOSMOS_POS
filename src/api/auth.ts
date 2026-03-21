import { apiRequest } from "@/api/http";
import type { CurrentSession, LoginRequest, LoginResponse } from "@/types/auth";

export function loginWithPin(payload: LoginRequest) {
  return apiRequest<LoginResponse>("/api/v1/auth/pin-login", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function fetchCurrentSession() {
  return apiRequest<CurrentSession>("/api/v1/auth/me");
}
