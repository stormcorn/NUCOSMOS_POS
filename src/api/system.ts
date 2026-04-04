import { apiRequest } from "@/api/http";
import type { DockerMaintenanceCleanup, DockerMaintenanceStatus, StorageStatus } from "@/types/system";

export function fetchStorageStatus() {
  return apiRequest<StorageStatus>("/api/v1/system/storage");
}

export function fetchDockerMaintenanceStatus() {
  return apiRequest<DockerMaintenanceStatus>("/api/v1/admin/system/docker-cache");
}

export function cleanupDockerCaches() {
  return apiRequest<DockerMaintenanceCleanup>("/api/v1/admin/system/docker-cache/cleanup", {
    method: "POST",
    body: {},
  });
}
