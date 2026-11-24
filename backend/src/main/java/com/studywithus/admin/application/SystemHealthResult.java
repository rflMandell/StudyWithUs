package com.studywithus.admin.application;

import java.time.Instant;
import java.util.List;

public record SystemHealthResult(
        String overallStatus,
        Instant checkedAt,
        DatabaseHealth database,
        RedisHealth redis,
        ObjectStorageHealth objectStorage,
        OutboxAndJobsHealth outboxAndJobs
) {
    public record DatabaseHealth(
            String status,
            long latencyMs,
            int activeConnections,
            int idleConnections,
            int totalConnections,
            int maxPoolSize,
            String error
    ) {}

    public record RedisHealth(
            String status,
            long latencyMs,
            String usedMemory,
            Long cachedKeys,
            String error
    ) {}

    public record ObjectStorageHealth(
            String status,
            String provider,
            String bucketOrPath,
            long latencyMs,
            String error
    ) {}

    public record OutboxAndJobsHealth(
            String status,
            long pendingOutboxEvents,
            long failedOutboxEvents,
            List<ScheduledJobStatus> recentJobs
    ) {}

    public record ScheduledJobStatus(
            String jobName,
            String status,
            Instant lastRunAt
    ) {}
}
