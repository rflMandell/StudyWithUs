package com.studywithus.moderation.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.moderation.domain.Report;
import com.studywithus.moderation.domain.ReportCreatedEvent;
import com.studywithus.moderation.domain.ReportRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateReportUseCase implements UseCase<CreateReportCommand, ReportResult> {

    private final ReportRepository reportRepository;
    private final CheckinRepository checkinRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReportResult execute(CreateReportCommand command) {
        userRepository.findById(command.reporterId())
                .filter(u -> u.isActive())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário denunciante não encontrado"));

        if (command.reason() == null || command.reason().isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_REASON", "O motivo da denúncia é obrigatório");
        }

        validateTargetExists(command);

        Report report = Report.create(
                command.reporterId(),
                command.targetType(),
                command.targetId(),
                command.reason().trim(),
                command.description() != null ? command.description().trim() : null
        );

        Report saved = reportRepository.save(report);

        eventPublisher.publishEvent(new ReportCreatedEvent(
                saved.getId(),
                saved.getReporterId(),
                saved.getTargetType(),
                saved.getTargetId(),
                saved.getReason()
        ));

        return ReportResult.fromDomain(saved);
    }

    private void validateTargetExists(CreateReportCommand command) {
        switch (command.targetType()) {
            case CHECKIN -> {
                Checkin checkin = checkinRepository.findById(command.targetId())
                        .filter(Checkin::isActive)
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in denunciado não foi encontrado"));
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(command.targetId())
                        .filter(Comment::isActive)
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comentário denunciado não foi encontrado"));
            }
            case USER -> {
                userRepository.findById(command.targetId())
                        .filter(u -> u.isActive())
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário denunciado não foi encontrado"));
            }
            case COMMUNITY -> {
                communityRepository.findById(command.targetId())
                        .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Comunidade denunciada não foi encontrada"));
            }
        }
    }
}
