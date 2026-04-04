package com.nucosmos.pos.backend.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Service
public class StorageStatusService {

    private final String monitoredPath;
    private final double warningThresholdPercent;
    private final double criticalThresholdPercent;

    public StorageStatusService(
            @Value("${app.monitoring.storage.path:/}") String monitoredPath,
            @Value("${app.monitoring.storage.warning-free-percent:15}") double warningThresholdPercent,
            @Value("${app.monitoring.storage.critical-free-percent:5}") double criticalThresholdPercent
    ) {
        this.monitoredPath = monitoredPath;
        this.warningThresholdPercent = warningThresholdPercent;
        this.criticalThresholdPercent = criticalThresholdPercent;
    }

    public StorageStatusResponse getStorageStatus() {
        try {
            Path path = Paths.get(monitoredPath).toAbsolutePath().normalize();
            FileStore fileStore = java.nio.file.Files.getFileStore(path);

            long totalBytes = fileStore.getTotalSpace();
            long usableBytes = fileStore.getUsableSpace();
            long usedBytes = Math.max(totalBytes - usableBytes, 0);
            double freePercent = totalBytes <= 0 ? 0 : (usableBytes * 100.0) / totalBytes;

            String level = resolveLevel(freePercent);
            String message = buildMessage(level, usableBytes, totalBytes, freePercent);

            return new StorageStatusResponse(
                    path.toString(),
                    totalBytes,
                    usableBytes,
                    usedBytes,
                    freePercent,
                    level,
                    warningThresholdPercent,
                    criticalThresholdPercent,
                    message,
                    Instant.now()
            );
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Unable to inspect server storage status",
                    ex
            );
        }
    }

    private String resolveLevel(double freePercent) {
        if (freePercent <= criticalThresholdPercent) {
            return "CRITICAL";
        }
        if (freePercent <= warningThresholdPercent) {
            return "WARNING";
        }
        return "OK";
    }

    private String buildMessage(String level, long usableBytes, long totalBytes, double freePercent) {
        String available = humanReadableBytes(usableBytes);
        String total = humanReadableBytes(totalBytes);
        String percent = String.format("%.1f%%", freePercent);

        return switch (level) {
            case "CRITICAL" -> "Disk space critical: " + available + " free of " + total + " (" + percent + ")";
            case "WARNING" -> "Disk space low: " + available + " free of " + total + " (" + percent + ")";
            default -> "Disk space healthy: " + available + " free of " + total + " (" + percent + ")";
        };
    }

    private String humanReadableBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }

        return String.format(unitIndex <= 1 ? "%.0f %s" : "%.1f %s", value, units[unitIndex]);
    }
}
