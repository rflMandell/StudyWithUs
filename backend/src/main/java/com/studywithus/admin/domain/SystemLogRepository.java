package com.studywithus.admin.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SystemLogRepository {
    SystemLog save(SystemLog log);
    List<SystemLog> findByFilter(
            String level,
            String module,
            String traceId,
            UUID userId,
            Integer statusCode,
            Instant from,
            Instant to,
            Instant cursorTimestamp,
            UUID cursorId,
            int limitPlusOne
    );
    List<SystemLog> findByTraceIdOrderByTimestampAsc(String traceId);
}
