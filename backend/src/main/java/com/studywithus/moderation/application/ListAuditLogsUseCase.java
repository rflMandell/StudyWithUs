package com.studywithus.moderation.application;

import com.studywithus.moderation.domain.AuditLogRepository;
import com.studywithus.shared.domain.PagedResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListAuditLogsUseCase {

    private final AuditLogRepository auditLogRepository;

    public ListAuditLogsUseCase(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public PagedResult<AuditLogResult> execute(ListAuditLogsQuery query) {
        var pagedLogs = auditLogRepository.findAll(query.targetType(), query.page(), query.size());
        var results = pagedLogs.content().stream()
                .map(AuditLogResult::fromDomain)
                .toList();

        return PagedResult.of(results, pagedLogs.page(), pagedLogs.size(), pagedLogs.totalElements());
    }
}
