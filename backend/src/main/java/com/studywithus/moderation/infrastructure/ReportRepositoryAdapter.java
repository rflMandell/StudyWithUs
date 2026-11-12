package com.studywithus.moderation.infrastructure;

import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportRepository;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportRepositoryAdapter implements ReportRepository {

    private final SpringDataReportRepository repository;

    @Override
    public Report save(Report report) {
        if (report.getId() == null) {
            report.setId(UUID.randomUUID());
        }
        if (report.getCreatedAt() == null) {
            report.setCreatedAt(Instant.now());
        }
        ReportJpaEntity entity = ReportJpaEntity.fromDomain(report);
        ReportJpaEntity saved = repository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Report> findById(UUID id) {
        return repository.findById(id).map(ReportJpaEntity::toDomain);
    }

    @Override
    public PagedResult<Report> findAll(ReportStatus status, ReportTargetType targetType, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<ReportJpaEntity> resultPage;

        if (status != null && targetType != null) {
            resultPage = repository.findByStatusAndTargetTypeOrderByCreatedAtDesc(status, targetType, pageRequest);
        } else if (status != null) {
            resultPage = repository.findByStatusOrderByCreatedAtDesc(status, pageRequest);
        } else if (targetType != null) {
            resultPage = repository.findByTargetTypeOrderByCreatedAtDesc(targetType, pageRequest);
        } else {
            resultPage = repository.findAllByOrderByCreatedAtDesc(pageRequest);
        }

        return PagedResult.of(
                resultPage.getContent().stream().map(ReportJpaEntity::toDomain).toList(),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements()
        );
    }

    @Override
    public long countPending() {
        return repository.countByStatus(ReportStatus.PENDING);
    }
}
