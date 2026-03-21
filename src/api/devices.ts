import { apiRequest } from "@/api/http";
import type { DeviceItem } from "@/types/device";
import type { StoreSummary } from "@/types/store";

export function fetchStores() {
  return apiRequest<StoreSummary[]>("/api/v1/admin/stores");
}

export function fetchDevices(query: { storeCode?: string; status?: string }) {
  return apiRequest<DeviceItem[]>("/api/v1/admin/devices", { query });
}
