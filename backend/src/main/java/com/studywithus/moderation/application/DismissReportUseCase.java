package com.studywithus.moderation.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMember;
import com.studywithus.community.domain.CommunityMemberRepository;
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
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DismissReportUseCase implements UseCase<DismissReportCommand, ReportResult> {

    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final CheckinRepository checkinRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommunityMemberRepository communityMemberRepository;
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

        validateModeratorPermission(report, command.reviewerId());

        report.dismiss(command.reviewerId());
        Report savedReport = reportRepository.save(report);

        String notes = command.notes() != null ? command.notes().replace("\"", "\\\"") : "";
        String metadata = "{\"action\":\"DISMISS\",\"notes\":\"" + notes + "\"}";
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

    private void validateModeratorPermission(Report report, UUID reviewerId) {
        switch (report.getTargetType()) {
            case COMMENT -> {
                Comment comment = commentRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comentário não encontrado"));
                Checkin checkin = checkinRepository.findById(comment.getCheckinId())
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));
                assertCommunityModerator(checkin.getCommunityId(), reviewerId);
            }
            case CHECKIN -> {
                Checkin checkin = checkinRepository.findById(report.getTargetId())
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));
                assertCommunityModerator(checkin.getCommunityId(), reviewerId);
            }
            case COMMUNITY -> {
                assertCommunityModerator(report.getTargetId(), reviewerId);
            }
            case USER -> {
                // Administrative context
            }
        }
    }

    private void assertCommunityModerator(UUID communityId, UUID reviewerId) {
        CommunityMember member = communityMemberRepository.findByCommunityIdAndUserId(communityId, reviewerId)
                .orElseThrow(() -> new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Você não é membro desta comunidade"));

        if (!member.isModeratorOrHigher()) {
            throw new DomainException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Apenas moderadores e administradores da comunidade podem avaliar denúncias");
        }
    }
}
