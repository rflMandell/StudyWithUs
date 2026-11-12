package com.studywithus.moderation.application;

import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListReportsUseCase implements UseCase<ListReportsQuery, PagedResult<ReportResult>> {

    private final ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<ReportResult> execute(ListReportsQuery query) {
        PagedResult<Report> paged = reportRepository.findAll(
                query.status(),
                query.targetType(),
                query.page(),
                query.size()
        );

        return PagedResult.of(
                paged.content().stream().map(ReportResult::fromDomain).toList(),
                paged.page(),
                paged.size(),
                paged.totalElements()
        );
    }
}
