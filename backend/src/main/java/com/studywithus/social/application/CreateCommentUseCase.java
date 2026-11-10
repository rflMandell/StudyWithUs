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

        boolean isMember = communityMemberRepository.isUserActiveMember(checkin.getCommunityId(), command.authorId());
        if (!isMember) {
            throw new DomainException(
                    HttpStatus.FORBIDDEN,
                    "COMMUNITY_NOT_A_MEMBER",
                    "Apenas membros ativos da comunidade podem comentar neste check-in"
            );
        }

        User author = userRepository.findById(command.authorId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuário não encontrado"));

        Comment comment = Comment.create(command.checkinId(), command.authorId(), command.content());
        Comment saved = commentRepository.save(comment);

        eventPublisher.publishEvent(new CommentCreatedEvent(
                saved.getId(),
                saved.getCheckinId(),
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
