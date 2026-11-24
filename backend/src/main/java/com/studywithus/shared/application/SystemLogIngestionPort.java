package com.studywithus.shared.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public interface SystemLogIngestionPort {
    void recordAsync(
            Instant timestamp,
            String level,
            String module,
            String traceId,
            String requestId,
            UUID userId,
            String httpMethod,
            String path,
            Integer statusCode,
            Long durationMs,
            String message,
            Map<String, Object> context
    );
}
