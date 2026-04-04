export type StorageStatus = {
  monitoredPath: string;
  totalBytes: number;
  usableBytes: number;
  usedBytes: number;
  freePercent: number;
  level: "OK" | "WARNING" | "CRITICAL" | string;
  warningThresholdPercent: number;
  criticalThresholdPercent: number;
  message: string;
  checkedAt: string;
};

export type DockerMaintenanceStatus = {
  enabled: boolean;
  available: boolean;
  dockerBinaryPath: string;
  dockerSocketPath: string;
  summary: string;
  details: string;
};

export type DockerMaintenanceCleanup = {
  executed: boolean;
  summary: string;
  beforeDetails: string;
  afterDetails: string;
  cleanupLog: string;
  executedAt: string;
};
