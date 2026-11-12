package com.studywithus.moderation.infrastructure;

import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.moderation.domain.AuditLogRepository;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository repository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(UUID.randomUUID());
        }
        if (auditLog.getCreatedAt() == null) {
            auditLog.setCreatedAt(Instant.now());
        }
        AuditLogJpaEntity entity = AuditLogJpaEntity.fromDomain(auditLog);
        AuditLogJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public PagedResult<AuditLog> findAll(String targetType, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<AuditLogJpaEntity> resultPage = (targetType != null && !targetType.isBlank())
                ? repository.findByTargetTypeOrderByCreatedAtDesc(targetType, pageRequest)
                : repository.findAllByOrderByCreatedAtDesc(pageRequest);

        return PagedResult.of(
                resultPage.getContent().stream().map(AuditLogJpaEntity::toDomain).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements()
        );
    }
}
