package com.studywithus.admin.application;

import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.moderation.domain.AuditLogRepository;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueryAdminAuditLogsUseCase {

    private static final int MAX_EXPORT_ROWS = 5000;
    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PagedResult<AuditLog> query(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            Instant from,
            Instant to,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return auditLogRepository.findByFilters(actorId, action, targetType, targetId, from, to, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> exportList(
            UUID actorId,
            String action,
            String targetType,
            UUID targetId,
            Instant from,
            Instant to
    ) {
        return auditLogRepository.findForExport(actorId, action, targetType, targetId, from, to, MAX_EXPORT_ROWS);
    }

    public String toCsv(List<AuditLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,createdAt,actorId,actorRole,action,targetType,targetId,reason,ipAddress,requestId,beforeState,afterState\n");
        for (AuditLog log : logs) {
            sb.append(csvCell(log.getId())).append(',')
              .append(csvCell(log.getCreatedAt())).append(',')
              .append(csvCell(log.getActorId())).append(',')
              .append(csvCell(log.getActorRole())).append(',')
              .append(csvCell(log.getAction())).append(',')
              .append(csvCell(log.getTargetType())).append(',')
              .append(csvCell(log.getTargetId())).append(',')
              .append(csvCell(log.getReason())).append(',')
              .append(csvCell(log.getIpAddress())).append(',')
              .append(csvCell(log.getRequestId())).append(',')
              .append(csvCell(log.getBeforeState())).append(',')
              .append(csvCell(log.getAfterState())).append('\n');
        }
        return sb.toString();
    }

    private String csvCell(Object val) {
        if (val == null) {
            return "";
        }
        String str = val.toString().replace("\"", "\"\"");
        return "\"" + str + "\"";
    }
}
