import { apiRequest } from "@/api/http";
import type { CurrentSession, LoginRequest, LoginResponse } from "@/types/auth";
import type { NavigationOrderPreference } from "@/types/navigation";

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

export function fetchNavigationPreference() {
  return apiRequest<NavigationOrderPreference>("/api/v1/auth/preferences/navigation");
}

export function saveNavigationPreference(payload: NavigationOrderPreference) {
  return apiRequest<NavigationOrderPreference>("/api/v1/auth/preferences/navigation", {
    method: "PUT",
    body: payload,
  });
}
