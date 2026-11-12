package com.studywithus.moderation.presentation;

import com.studywithus.moderation.application.ListAuditLogsQuery;
import com.studywithus.moderation.application.ListAuditLogsUseCase;
import com.studywithus.shared.presentation.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final ListAuditLogsUseCase listAuditLogsUseCase;

    @GetMapping
    public ResponseEntity<PagedResponse<AuditLogResponse>> listAuditLogs(
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var query = new ListAuditLogsQuery(targetType, page, size);
        var pagedResult = listAuditLogsUseCase.execute(query);
        return ResponseEntity.ok(PagedResponse.from(pagedResult, AuditLogResponse::from));
    }
}
