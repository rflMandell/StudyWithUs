package com.studywithus.moderation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.media.domain.MediaAsset;
import com.studywithus.media.domain.MediaAssetRepository;
import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.moderation.domain.AuditLogRepository;
import com.studywithus.moderation.domain.ModerationAction;
import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportRepository;
import com.studywithus.moderation.domain.ReportResolvedEvent;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
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

import java.util.LinkedHashMap;
import java.util.Map;
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
    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectStoragePort objectStoragePort;
    private final CreateInAppNotificationUseCase createInAppNotificationUseCase;
    private final ModerationAuthorizationService moderationAuthorizationService;
    private final ObjectMapper objectMapper;
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

        moderationAuthorizationService.assertCanModerateTarget(report.getTargetType(), report.getTargetId(), command.reviewerId());

        Map<String, Object> snapshot = applyModerationAction(report, command);

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

    private Map<String, Object> applyModerationAction(Report report, ResolveReportCommand command) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        switch (command.action()) {
            case REMOVE_CONTENT -> {
                if (report.getTargetType() == ReportTargetType.COMMENT) {
                    Comment comment = commentRepository.findById(report.getTargetId()).orElse(null);
                    if (comment != null) {
                        snapshot.put("content", comment.getContent());
                        snapshot.put("authorId", comment.getAuthorId().toString());
                        comment.removeByModerator();
                        commentRepository.save(comment);
                        log.info("Comment {} removed by moderator {}", comment.getId(), command.reviewerId());
                    }
                } else if (report.getTargetType() == ReportTargetType.CHECKIN) {
                    Checkin checkin = checkinRepository.findById(report.getTargetId()).orElse(null);
                    if (checkin != null) {
                        snapshot.put("caption", checkin.getCaption());
                        snapshot.put("userId", checkin.getUserId().toString());
                        checkin.delete();
                        checkinRepository.save(checkin);
                        log.info("Check-in {} removed by moderator {}", checkin.getId(), command.reviewerId());
                    }
                } else if (report.getTargetType() == ReportTargetType.MEDIA) {
                    MediaAsset asset = mediaAssetRepository.findById(report.getTargetId()).orElse(null);
                    if (asset != null) {
                        snapshot.put("storageKey", asset.getStorageKey());
                        snapshot.put("ownerId", asset.getOwnerId().toString());
                        asset.reject("Removed by moderator: " + report.getReason());
                        mediaAssetRepository.save(asset);
                        try {
                            objectStoragePort.delete(asset.getStorageKey());
                            if (asset.getThumbnailKey() != null) {
                                objectStoragePort.delete(asset.getThumbnailKey());
                            }
                        } catch (Exception ex) {
                            log.warn("Failed to delete moderated media object {}: {}", asset.getStorageKey(), ex.getMessage());
                        }
                        log.info("Media {} removed by moderator {}", asset.getId(), command.reviewerId());
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
                if ((report.getTargetType() == ReportTargetType.CHECKIN
                        || report.getTargetType() == ReportTargetType.COMMENT
                        || report.getTargetType() == ReportTargetType.COMMUNITY)
                        && !moderationAuthorizationService.isPlatformAdmin()) {
                    throw new DomainException(
                            HttpStatus.FORBIDDEN,
                            "FORBIDDEN",
                            "Moderadores de comunidade não podem suspender contas globalmente"
                    );
                }
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
            case MEDIA -> mediaAssetRepository.findById(report.getTargetId()).map(MediaAsset::getOwnerId).orElse(null);
        };
    }

    private String buildAuditMetadata(ResolveReportCommand command, Report report, Map<String, Object> snapshot) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("action", command.action().name());
        metadata.put("notes", command.notes() != null ? command.notes() : "");
        metadata.put("reason", report.getReason());
        metadata.put("snapshot", snapshot);
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return "{}";
        }
    }
}
