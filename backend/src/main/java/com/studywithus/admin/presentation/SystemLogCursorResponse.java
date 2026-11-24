package com.studywithus.admin.presentation;

import java.util.List;

public record SystemLogCursorResponse(
        List<SystemLogResponse> items,
        String nextCursor,
        boolean hasNext
) {
}
