package com.studywithus.admin.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.admin.domain.SystemLog;
import com.studywithus.admin.domain.SystemLogRepository;
import com.studywithus.shared.application.SystemLogIngestionPort;
import com.studywithus.shared.infrastructure.logging.SensitiveDataMasker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Non-blocking asynchronous system log ingestion pipeline.
 * Failures during log persistence are swallowed and reported only to stderr/fallback logger
 * so they never impact main application requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemLogIngestionService implements SystemLogIngestionPort {

    private final SystemLogRepository systemLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    @Override
    public void recordAsync(
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
    ) {
        try {
            String maskedMessage = SensitiveDataMasker.mask(message != null ? message : "");
            String maskedContext = serializeAndMaskContext(context);

            SystemLog entry = SystemLog.builder()
                    .id(UUID.randomUUID())
                    .timestamp(timestamp != null ? timestamp : Instant.now())
                    .level(level != null ? level : "INFO")
                    .module(module != null ? module : "shared")
                    .traceId(traceId != null ? traceId : "unknown")
                    .requestId(requestId)
                    .userId(userId)
                    .httpMethod(httpMethod)
                    .path(SensitiveDataMasker.mask(path))
                    .statusCode(statusCode)
                    .durationMs(durationMs)
                    .message(maskedMessage)
                    .context(maskedContext)
                    .build();

            systemLogRepository.save(entry);
        } catch (Exception e) {
            log.warn("Non-blocking system log persistence failed (ignored): {}", e.getMessage());
        }
    }

    private String serializeAndMaskContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        try {
            String rawJson = objectMapper.writeValueAsString(context);
            return SensitiveDataMasker.mask(rawJson);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
