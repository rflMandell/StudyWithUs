package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentLikeRepository;
import com.studywithus.social.domain.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListCommentsUseCase {

    private final CommentRepository commentRepository;
    private final CheckinRepository checkinRepository;
    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Autowired
    public ListCommentsUseCase(
            CommentRepository commentRepository,
            CheckinRepository checkinRepository,
            CommunityRepository communityRepository,
            CommunityMemberRepository communityMemberRepository,
            UserRepository userRepository,
            @Autowired(required = false) CommentLikeRepository commentLikeRepository
    ) {
        this.commentRepository = commentRepository;
        this.checkinRepository = checkinRepository;
        this.communityRepository = communityRepository;
        this.communityMemberRepository = communityMemberRepository;
        this.userRepository = userRepository;
        this.commentLikeRepository = commentLikeRepository;
    }

    public ListCommentsUseCase(
            CommentRepository commentRepository,
            CheckinRepository checkinRepository,
            CommunityRepository communityRepository,
            CommunityMemberRepository communityMemberRepository,
            UserRepository userRepository
    ) {
        this(commentRepository, checkinRepository, communityRepository, communityMemberRepository, userRepository, null);
    }

    @Transactional(readOnly = true)
    public PagedResult<CommentResult> execute(ListCommentsQuery query) {
        Checkin checkin = checkinRepository.findById(query.checkinId())
                .filter(Checkin::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));

        Community community = communityRepository.findById(checkin.getCommunityId())
                .filter(Community::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "COMMUNITY_NOT_FOUND", "Comunidade não encontrada"));

        if (community.getVisibility() == CommunityVisibility.PRIVATE) {
            if (query.callerId() == null || !communityMemberRepository.isUserActiveMember(community.getId(), query.callerId())) {
                throw new DomainException(
                        HttpStatus.FORBIDDEN,
                        "COMMUNITY_NOT_A_MEMBER",
                        "Apenas membros ativos da comunidade podem visualizar comentários deste check-in"
                );
            }
        }

        int page = Math.max(0, query.page());
        int size = query.size() > 0 ? Math.min(query.size(), 100) : 20;

        PagedResult<Comment> pagedComments = commentRepository.findByCheckinId(checkin.getId(), page, size);
        if (pagedComments.content().isEmpty()) {
            return new PagedResult<>(
                    Collections.emptyList(),
                    pagedComments.page(),
                    pagedComments.size(),
                    pagedComments.totalElements(),
                    pagedComments.totalPages(),
                    pagedComments.hasNext()
            );
        }

        Set<UUID> authorIds = pagedComments.content().stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        List<UUID> commentIds = pagedComments.content().stream()
                .map(Comment::getId)
                .toList();

        Map<UUID, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<UUID, Long> likesMap = commentLikeRepository != null
                ? commentLikeRepository.countByCommentIds(commentIds)
                : Collections.emptyMap();

        Set<UUID> likedIds = commentLikeRepository != null && query.callerId() != null
                ? commentLikeRepository.findLikedCommentIdsByUser(commentIds, query.callerId())
                : Collections.emptySet();

        List<CommentResult> results = pagedComments.content().stream()
                .map(c -> {
                    User author = authorMap.get(c.getAuthorId());
                    return CommentResult.fromDomain(
                            c,
                            author != null ? author.getUsername() : "usuario",
                            author != null ? author.getName() : "Usuário",
                            author != null ? author.getAvatarUrl() : null,
                            likesMap.getOrDefault(c.getId(), 0L),
                            likedIds.contains(c.getId())
                    );
                })
                .toList();

        return new PagedResult<>(
                results,
                pagedComments.page(),
                pagedComments.size(),
                pagedComments.totalElements(),
                pagedComments.totalPages(),
                pagedComments.hasNext()
        );
    }
}
