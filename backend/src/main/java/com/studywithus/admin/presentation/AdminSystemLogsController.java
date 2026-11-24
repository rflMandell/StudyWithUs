package com.studywithus.admin.presentation;

import com.studywithus.admin.application.QuerySystemLogsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/system-logs")
@RequiredArgsConstructor
public class AdminSystemLogsController {

    private final QuerySystemLogsUseCase querySystemLogsUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<SystemLogCursorResponse> listSystemLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Integer statusCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "30") int limit
    ) {
        QuerySystemLogsUseCase.CursorPage page = querySystemLogsUseCase.queryLogs(
                level, module, traceId, userId, statusCode, from, to, cursor, limit
        );
        List<SystemLogResponse> items = page.items().stream().map(SystemLogResponse::from).toList();
        return ResponseEntity.ok(new SystemLogCursorResponse(items, page.nextCursor(), page.hasNext()));
    }

    @GetMapping("/{traceId}")
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<SystemLogResponse>> getTraceTimeline(@PathVariable String traceId) {
        List<SystemLogResponse> logs = querySystemLogsUseCase.getTraceLogs(traceId).stream()
                .map(SystemLogResponse::from)
                .toList();
        return ResponseEntity.ok(logs);
    }
}
