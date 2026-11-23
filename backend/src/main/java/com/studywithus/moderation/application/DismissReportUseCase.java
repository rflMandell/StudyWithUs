package com.studywithus.moderation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.moderation.domain.AuditLogRepository;
import com.studywithus.moderation.domain.ModerationAction;
import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportRepository;
import com.studywithus.moderation.domain.ReportResolvedEvent;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DismissReportUseCase implements UseCase<DismissReportCommand, ReportResult> {

    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ModerationAuthorizationService moderationAuthorizationService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReportResult execute(DismissReportCommand command) {
        userRepository.findById(command.reviewerId())
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Moderador não encontrado"));

        Report report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Denúncia não encontrada"));

        if (!report.isPending()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "REPORT_ALREADY_REVIEWED", "Esta denúncia já foi avaliada anteriormente");
        }

        moderationAuthorizationService.assertCanModerateTarget(report.getTargetType(), report.getTargetId(), command.reviewerId());

        report.dismiss(command.reviewerId());
        Report savedReport = reportRepository.save(report);

        String metadata = buildAuditMetadata(command.notes());
        AuditLog auditLog = AuditLog.create(
                command.reviewerId(),
                "REPORT_DISMISSED",
                report.getTargetType().name(),
                report.getTargetId(),
                metadata
        );
        auditLogRepository.save(auditLog);

        eventPublisher.publishEvent(new ReportResolvedEvent(
                savedReport.getId(),
                command.reviewerId(),
                ReportStatus.DISMISSED,
                ModerationAction.DISMISS,
                savedReport.getTargetType(),
                savedReport.getTargetId()
        ));

        return ReportResult.fromDomain(savedReport);
    }

    private String buildAuditMetadata(String notes) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", "DISMISS");
        metadata.put("notes", notes != null ? notes : "");
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return "{\"action\":\"DISMISS\"}";
        }
    }
}
