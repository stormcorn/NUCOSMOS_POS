import { apiRequest } from "@/api/http";
import type {
  AuthStore,
  CurrentSession,
  LoginRequest,
  LoginResponse,
  RegistrationCompleteRequest,
  RegistrationCompleteResponse,
  RegistrationStartRequest,
  RegistrationStartResponse,
} from "@/types/auth";
import type { NavigationOrderPreference } from "@/types/navigation";

export function loginWithPin(payload: LoginRequest) {
  return apiRequest<LoginResponse>("/api/v1/auth/pin-login", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function fetchAvailableStores() {
  return apiRequest<AuthStore[]>("/api/v1/auth/stores", {
    auth: false,
  });
}

export function startRegistration(payload: RegistrationStartRequest) {
  return apiRequest<RegistrationStartResponse>("/api/v1/auth/register/start", {
    method: "POST",
    auth: false,
    body: payload,
  });
}

export function completeRegistration(payload: RegistrationCompleteRequest) {
  return apiRequest<RegistrationCompleteResponse>("/api/v1/auth/register/complete", {
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
