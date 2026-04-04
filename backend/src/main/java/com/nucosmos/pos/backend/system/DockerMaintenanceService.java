package com.nucosmos.pos.backend.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DockerMaintenanceService {

    private static final long COMMAND_TIMEOUT_SECONDS = 120;

    private final boolean enabled;
    private final String dockerBinaryPath;
    private final String dockerSocketPath;

    public DockerMaintenanceService(
            @Value("${app.maintenance.docker-cache.enabled:false}") boolean enabled,
            @Value("${app.maintenance.docker-cache.docker-binary:/usr/bin/docker}") String dockerBinaryPath,
            @Value("${app.maintenance.docker-cache.socket-path:/var/run/docker.sock}") String dockerSocketPath
    ) {
        this.enabled = enabled;
        this.dockerBinaryPath = dockerBinaryPath;
        this.dockerSocketPath = dockerSocketPath;
    }

    public DockerMaintenanceStatusResponse getStatus() {
        boolean binaryExists = Files.isExecutable(Paths.get(dockerBinaryPath));
        boolean socketExists = Files.exists(Paths.get(dockerSocketPath));
        boolean available = enabled && binaryExists && socketExists;

        String summary;
        String details = "";

        if (!enabled) {
            summary = "Docker cache cleanup is disabled on this server.";
        } else if (!binaryExists) {
            summary = "Docker CLI is not available inside the backend container.";
        } else if (!socketExists) {
            summary = "Docker socket is not mounted into the backend container.";
        } else {
            summary = "Safe Docker cache cleanup is available for ADMIN users.";
            details = runCommand(List.of(dockerBinaryPath, "system", "df")).output();
        }

        return new DockerMaintenanceStatusResponse(
                enabled,
                available,
                dockerBinaryPath,
                dockerSocketPath,
                summary,
                details
        );
    }

    public DockerMaintenanceCleanupResponse cleanupCaches() {
        DockerMaintenanceStatusResponse status = getStatus();
        if (!status.available()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, status.summary());
        }

        String before = status.details();
        StringBuilder cleanupLog = new StringBuilder();
        cleanupLog.append(runCommand(List.of(dockerBinaryPath, "builder", "prune", "-af")).output()).append(System.lineSeparator());
        cleanupLog.append(runCommand(List.of(dockerBinaryPath, "image", "prune", "-af")).output()).append(System.lineSeparator());
        cleanupLog.append(runCommand(List.of(dockerBinaryPath, "container", "prune", "-f")).output()).append(System.lineSeparator());
        String after = runCommand(List.of(dockerBinaryPath, "system", "df")).output();

        return new DockerMaintenanceCleanupResponse(
                true,
                "Docker cache cleanup completed successfully.",
                before,
                after,
                cleanupLog.toString().trim(),
                Instant.now()
        );
    }

    private CommandResult runCommand(List<String> command) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        try {
            Process process = builder.start();
            String output = readOutput(process);

            boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Maintenance command timed out");
            }

            if (process.exitValue() != 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Maintenance command failed: " + output.trim()
                );
            }

            return new CommandResult(output.trim());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to execute maintenance command", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Maintenance command interrupted", ex);
        }
    }

    private String readOutput(Process process) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return String.join(System.lineSeparator(), lines);
    }

    private record CommandResult(String output) {
    }
}
