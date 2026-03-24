import type { ApiErrorResponse, ApiResponse } from "@/types/api";

const CONFIGURED_API_BASE_URL = import.meta.env.VITE_API_BASE_URL as string | undefined;
const API_BASE_URL = CONFIGURED_API_BASE_URL && CONFIGURED_API_BASE_URL.trim().length > 0
  ? CONFIGURED_API_BASE_URL
  : window.location.origin;
const ACCESS_TOKEN_KEY = "nucosmos.admin.accessToken";

export class ApiError extends Error {
  status: number;
  details: string[];

  constructor(status: number, message: string, details: string[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  query?: Record<string, string | number | boolean | null | undefined>;
  auth?: boolean;
};

function buildUrl(path: string, query?: RequestOptions["query"]) {
  const url = new URL(path, API_BASE_URL);

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    }
  }

  return url.toString();
}

function getAccessToken() {
  return window.localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function setStoredAccessToken(token: string | null) {
  if (token) {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
    return;
  }

  window.localStorage.removeItem(ACCESS_TOKEN_KEY);
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}) {
  const { body, headers, query, auth = true, ...rest } = options;
  const token = auth ? getAccessToken() : null;
  const requestHeaders = new Headers(headers);

  if (!requestHeaders.has("Content-Type") && body !== undefined) {
    requestHeaders.set("Content-Type", "application/json");
  }

  if (token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(buildUrl(path, query), {
    ...rest,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!response.ok) {
    let errorPayload: ApiErrorResponse | null = null;

    try {
      errorPayload = (await response.json()) as ApiErrorResponse;
    } catch {
      errorPayload = null;
    }

    throw new ApiError(
      response.status,
      errorPayload?.message ?? `Request failed with status ${response.status}`,
      errorPayload?.details ?? [],
    );
  }

  const payload = (await response.json()) as ApiResponse<T>;
  return payload.data;
}
