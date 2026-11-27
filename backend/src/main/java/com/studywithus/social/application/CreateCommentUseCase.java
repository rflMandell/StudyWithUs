package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentCreatedEvent;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateCommentUseCase implements UseCase<CreateCommentCommand, CommentResult> {

    private final CommentRepository commentRepository;
    private final CheckinRepository checkinRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CommentResult execute(CreateCommentCommand command) {
        Checkin checkin = checkinRepository.findById(command.checkinId())
                .filter(Checkin::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));

        if (checkin.getCommunityId() != null) {
            boolean isMember = communityMemberRepository.isUserActiveMember(checkin.getCommunityId(), command.authorId());
            if (!isMember) {
                throw new DomainException(
                        HttpStatus.FORBIDDEN,
                        "COMMUNITY_NOT_A_MEMBER",
                        "Apenas membros ativos da comunidade podem comentar neste check-in"
                );
            }
        }

        if (command.parentCommentId() != null) {
            Comment parentComment = commentRepository.findById(command.parentCommentId())
                    .filter(Comment::isActive)
                    .orElseThrow(() -> new DomainException(
                            HttpStatus.NOT_FOUND,
                            "PARENT_COMMENT_NOT_FOUND",
                            "Comentário pai não encontrado"
                    ));

            if (!parentComment.getCheckinId().equals(command.checkinId())) {
                throw new DomainException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_PARENT_COMMENT",
                        "O comentário pai não pertence a este check-in"
                );
            }
        }

        User author = userRepository.findById(command.authorId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário não encontrado"));

        Comment comment = Comment.create(
                command.checkinId(),
                command.authorId(),
                command.content(),
                command.parentCommentId()
        );
        Comment saved = commentRepository.save(comment);

        eventPublisher.publishEvent(new CommentCreatedEvent(
                saved.getId(),
                saved.getCheckinId(),
                saved.getParentCommentId(),
                saved.getAuthorId(),
                checkin.getCommunityId()
        ));

        return CommentResult.fromDomain(
                saved,
                author.getUsername(),
                author.getName(),
                author.getAvatarUrl()
        );
    }
}
