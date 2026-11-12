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
import com.studywithus.notification.application.CreateInAppNotificationUseCase;
import com.studywithus.notification.application.CreateNotificationCommand;
import com.studywithus.notification.domain.NotificationType;
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

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResolveReportUseCase implements UseCase<ResolveReportCommand, ReportResult> {

    private final ReportRepository reportRepository;
    private final AuditLogRepository auditLogRepository;
    private final CheckinRepository checkinRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CreateInAppNotificationUseCase createInAppNotificationUseCase;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReportResult execute(ResolveReportCommand command) {
        userRepository.findById(command.reviewerId())
                .filter(User::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Moderador não encontrado"));

        Report report = reportRepository.findById(command.reportId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Denúncia não encontrada"));

        if (!report.isPending()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "REPORT_ALREADY_REVIEWED", "Esta denúncia já foi avaliada anteriormente");
        }

        validateModeratorPermission(report, command.reviewerId());

        String snapshot = applyModerationAction(report, command);

        report.resolve(command.reviewerId());
        Report savedReport = reportRepository.save(report);

        // Mandatory Audit Trail
        String metadata = buildAuditMetadata(command, report, snapshot);
        AuditLog auditLog = AuditLog.create(
                command.reviewerId(),
                "REPORT_RESOLVED",
                report.getTargetType().name(),
                report.getTargetId(),
                metadata
        );
        auditLogRepository.save(auditLog);
        log.info("Audit log recorded: actor={}, action=REPORT_RESOLVED, targetType={}, targetId={}",
                command.reviewerId(), report.getTargetType(), report.getTargetId());

        eventPublisher.publishEvent(new ReportResolvedEvent(
                savedReport.getId(),
                command.reviewerId(),
                ReportStatus.RESOLVED,
                command.action(),
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
                // Any active user with administrative context can moderate users
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

    private String applyModerationAction(Report report, ResolveReportCommand command) {
        String snapshot = "{}";
        switch (command.action()) {
            case REMOVE_CONTENT -> {
                if (report.getTargetType() == com.studywithus.moderation.domain.ReportTargetType.COMMENT) {
                    Comment comment = commentRepository.findById(report.getTargetId()).orElse(null);
                    if (comment != null) {
                        snapshot = "{\"content\":\"" + escapeJson(comment.getContent()) + "\",\"authorId\":\"" + comment.getAuthorId() + "\"}";
                        comment.removeByModerator();
                        commentRepository.save(comment);
                        log.info("Comment {} removed by moderator {}", comment.getId(), command.reviewerId());
                    }
                } else if (report.getTargetType() == com.studywithus.moderation.domain.ReportTargetType.CHECKIN) {
                    Checkin checkin = checkinRepository.findById(report.getTargetId()).orElse(null);
                    if (checkin != null) {
                        snapshot = "{\"caption\":\"" + escapeJson(checkin.getCaption()) + "\",\"userId\":\"" + checkin.getUserId() + "\"}";
                        checkin.delete();
                        checkinRepository.save(checkin);
                        log.info("Check-in {} removed by moderator {}", checkin.getId(), command.reviewerId());
                    }
                }
            }
            case WARN_USER -> {
                UUID offendingUserId = findOffendingUserId(report);
                if (offendingUserId != null) {
                    createInAppNotificationUseCase.execute(new CreateNotificationCommand(
                            offendingUserId,
                            NotificationType.NEW_CHECKIN, // In-app notification alert
                            "Aviso de Moderação",
                            "Seu conteúdo foi sinalizado pela moderação por violação das diretrizes comunitárias: " + report.getReason(),
                            report.getTargetType().name(),
                            report.getTargetId()
                    ));
                }
            }
            case SUSPEND_USER -> {
                UUID offendingUserId = findOffendingUserId(report);
                if (offendingUserId != null) {
                    userRepository.findById(offendingUserId).ifPresent(u -> {
                        u.setStatus(com.studywithus.identity.domain.UserStatus.SUSPENDED);
                        userRepository.save(u);
                        log.info("User {} suspended by moderator {}", offendingUserId, command.reviewerId());
                    });
                }
            }
            case DISMISS -> {
                // Handled gracefully without altering content
            }
        }
        return snapshot;
    }

    private UUID findOffendingUserId(Report report) {
        return switch (report.getTargetType()) {
            case COMMENT -> commentRepository.findById(report.getTargetId()).map(Comment::getAuthorId).orElse(null);
            case CHECKIN -> checkinRepository.findById(report.getTargetId()).map(Checkin::getUserId).orElse(null);
            case USER -> report.getTargetId();
            case COMMUNITY -> null;
        };
    }

    private String buildAuditMetadata(ResolveReportCommand command, Report report, String snapshot) {
        return "{\"action\":\"" + command.action().name() + "\","
                + "\"notes\":\"" + escapeJson(command.notes() != null ? command.notes() : "") + "\","
                + "\"reason\":\"" + escapeJson(report.getReason()) + "\","
                + "\"snapshot\":" + snapshot + "}";
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
