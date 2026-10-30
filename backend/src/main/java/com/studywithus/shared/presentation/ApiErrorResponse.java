package com.studywithus.shared.presentation;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public record ApiErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId
) {
    public static ApiErrorResponse of(int status, String code, String message, String path, String traceId) {
        return new ApiErrorResponse(Instant.now(), status, code, message, path, traceId);
    }
}
