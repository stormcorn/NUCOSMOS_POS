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
