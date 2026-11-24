package com.studywithus.admin.application;

import java.util.List;

public record SystemInfoResult(
        String applicationName,
        String version,
        String commitSha,
        List<String> activeProfiles,
        long uptimeSeconds,
        String javaVersion,
        String jvmName,
        FlywayStatus flyway
) {
    public record FlywayStatus(
            boolean enabled,
            String currentVersion,
            int appliedMigrations,
            int pendingMigrations
    ) {}
}
