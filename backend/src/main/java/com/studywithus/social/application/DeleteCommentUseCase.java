package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentDeletedEvent;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteCommentUseCase {

    private final CommentRepository commentRepository;
    private final CheckinRepository checkinRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(DeleteCommentCommand command) {
        Comment comment = commentRepository.findById(command.commentId())
                .filter(Comment::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comentário não encontrado"));

        comment.delete(command.requestingUserId());
        commentRepository.save(comment);

        Checkin checkin = checkinRepository.findById(comment.getCheckinId()).orElse(null);
        UUID communityId = checkin != null ? checkin.getCommunityId() : null;

        eventPublisher.publishEvent(new CommentDeletedEvent(
                comment.getId(),
                comment.getCheckinId(),
                comment.getAuthorId(),
                communityId
        ));
    }
}
