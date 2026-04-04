import { apiRequest } from "@/api/http";
import type { StorageStatus } from "@/types/system";

export function fetchStorageStatus() {
  return apiRequest<StorageStatus>("/api/v1/system/storage");
}
