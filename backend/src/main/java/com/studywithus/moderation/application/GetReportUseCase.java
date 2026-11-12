package com.studywithus.moderation.application;

import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetReportUseCase implements UseCase<UUID, ReportResult> {

    private final ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public ReportResult execute(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Denúncia não encontrada"));
        return ReportResult.fromDomain(report);
    }
}
