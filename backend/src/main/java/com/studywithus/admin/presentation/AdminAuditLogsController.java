package com.studywithus.admin.presentation;

import com.studywithus.admin.application.QueryAdminAuditLogsUseCase;
import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.shared.presentation.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogsController {

    private final QueryAdminAuditLogsUseCase queryAdminAuditLogsUseCase;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPPORT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<AdminAuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResult<AuditLog> result = queryAdminAuditLogsUseCase.query(
                actorId, action, targetType, targetId, from, to, page, size
        );
        return ResponseEntity.ok(PagedResponse.from(result, AdminAuditLogResponse::from));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<?> exportAuditLogs(
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) UUID targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "csv") String format
    ) {
        List<AuditLog> logs = queryAdminAuditLogsUseCase.exportList(
                actorId, action, targetType, targetId, from, to
        );

        if ("json".equalsIgnoreCase(format)) {
            List<AdminAuditLogResponse> payload = logs.stream().map(AdminAuditLogResponse::from).toList();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.json\"")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload);
        }

        String csv = queryAdminAuditLogsUseCase.toCsv(logs);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-logs.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
