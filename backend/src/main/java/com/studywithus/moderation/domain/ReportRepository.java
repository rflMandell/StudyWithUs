package com.studywithus.moderation.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.Optional;
import java.util.UUID;

public interface ReportRepository {
    Report save(Report report);
    Optional<Report> findById(UUID id);
    PagedResult<Report> findAll(ReportStatus status, ReportTargetType targetType, int page, int size);
    long countPending();
}
