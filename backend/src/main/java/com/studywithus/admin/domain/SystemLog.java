package com.studywithus.admin.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLog {
    private UUID id;
    private Instant timestamp;
    private String level;
    private String module;
    private String traceId;
    private String requestId;
    private UUID userId;
    private String httpMethod;
    private String path;
    private Integer statusCode;
    private Long durationMs;
    private String message;
    private String context;
}
