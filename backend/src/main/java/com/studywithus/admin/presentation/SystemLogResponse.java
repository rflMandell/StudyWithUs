package com.studywithus.admin.presentation;

import com.studywithus.admin.domain.SystemLog;

import java.time.Instant;
import java.util.UUID;

public record SystemLogResponse(
        UUID id,
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
        String context
) {
    public static SystemLogResponse from(SystemLog log) {
        return new SystemLogResponse(
                log.getId(),
                log.getTimestamp(),
                log.getLevel(),
                log.getModule(),
                log.getTraceId(),
                log.getRequestId(),
                log.getUserId(),
                log.getHttpMethod(),
                log.getPath(),
                log.getStatusCode(),
                log.getDurationMs(),
                log.getMessage(),
                log.getContext()
        );
    }
}
