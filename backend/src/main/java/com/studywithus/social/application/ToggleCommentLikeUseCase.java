package com.studywithus.social.application;

import com.studywithus.checkin.application.LikeToggleResult;
import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.shared.application.UseCase;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentLikeRepository;
import com.studywithus.social.domain.CommentLikedEvent;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToggleCommentLikeUseCase implements UseCase<ToggleCommentLikeCommand, LikeToggleResult> {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CheckinRepository checkinRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LikeToggleResult execute(ToggleCommentLikeCommand command) {
        Comment comment = commentRepository.findById(command.commentId())
                .filter(Comment::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "Comentário não encontrado"));

        Checkin checkin = checkinRepository.findById(comment.getCheckinId())
                .filter(Checkin::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));

        if (checkin.getCommunityId() != null) {
            Community community = communityRepository.findById(checkin.getCommunityId())
                    .filter(Community::isActive)
                    .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Comunidade não encontrada"));

            if (community.isPrivate() && !communityMemberRepository.isUserActiveMember(community.getId(), command.userId())) {
                throw new DomainException(
                        HttpStatus.FORBIDDEN,
                        "COMMUNITY_NOT_A_MEMBER",
                        "Apenas membros ativos da comunidade podem curtir este comentário"
                );
            }
        }

        boolean alreadyLiked = commentLikeRepository.exists(comment.getId(), command.userId());
        boolean liked;
        if (alreadyLiked) {
            commentLikeRepository.delete(comment.getId(), command.userId());
            liked = false;
        } else {
            commentLikeRepository.save(comment.getId(), command.userId());
            liked = true;
            eventPublisher.publishEvent(new CommentLikedEvent(
                    comment.getId(),
                    comment.getCheckinId(),
                    comment.getAuthorId(),
                    command.userId(),
                    checkin.getCommunityId()
            ));
        }

        long likesCount = commentLikeRepository.countByCommentId(comment.getId());
        return new LikeToggleResult(comment.getId(), liked, likesCount);
    }
}
