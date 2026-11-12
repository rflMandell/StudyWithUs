package com.studywithus.moderation.application;

import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;

public record ListReportsQuery(
        ReportStatus status,
        ReportTargetType targetType,
        int page,
        int size
) {
    public ListReportsQuery {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
    }
}
