package com.studywithus.moderation.application;

public record ListAuditLogsQuery(
        String targetType,
        int page,
        int size
) {
    public ListAuditLogsQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
    }
}
