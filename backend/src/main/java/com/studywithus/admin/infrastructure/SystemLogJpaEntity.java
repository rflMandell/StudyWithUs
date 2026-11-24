package com.studywithus.admin.infrastructure;

import com.studywithus.admin.domain.SystemLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 10)
    private String level;

    @Column(nullable = false, length = 80)
    private String module;

    @Column(name = "trace_id", nullable = false, length = 64)
    private String traceId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(length = 500)
    private String path;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context")
    private String context;

    public SystemLog toDomain() {
        return SystemLog.builder()
                .id(id)
                .timestamp(timestamp)
                .level(level)
                .module(module)
                .traceId(traceId)
                .requestId(requestId)
                .userId(userId)
                .httpMethod(httpMethod)
                .path(path)
                .statusCode(statusCode)
                .durationMs(durationMs)
                .message(message)
                .context(context)
                .build();
    }

    public static SystemLogJpaEntity fromDomain(SystemLog log) {
        return SystemLogJpaEntity.builder()
                .id(log.getId() != null ? log.getId() : UUID.randomUUID())
                .timestamp(log.getTimestamp() != null ? log.getTimestamp() : Instant.now())
                .level(log.getLevel())
                .module(log.getModule())
                .traceId(log.getTraceId())
                .requestId(log.getRequestId())
                .userId(log.getUserId())
                .httpMethod(log.getHttpMethod())
                .path(log.getPath())
                .statusCode(log.getStatusCode())
                .durationMs(log.getDurationMs())
                .message(log.getMessage())
                .context(log.getContext())
                .build();
    }
}
