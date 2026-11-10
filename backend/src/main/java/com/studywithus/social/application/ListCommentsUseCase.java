package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.domain.PagedResult;
import com.studywithus.social.domain.Comment;
import com.studywithus.social.domain.CommentRepository;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ListCommentsUseCase {

    private final CommentRepository commentRepository;
    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PagedResult<CommentResult> execute(ListCommentsQuery query) {
        Checkin checkin = checkinRepository.findById(query.checkinId())
                .filter(Checkin::isActive)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "CHECKIN_NOT_FOUND", "Check-in não encontrado"));

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

        Map<UUID, User> authorMap = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<CommentResult> results = pagedComments.content().stream()
                .map(c -> {
                    User author = authorMap.get(c.getAuthorId());
                    return CommentResult.fromDomain(
                            c,
                            author != null ? author.getUsername() : "usuario",
                            author != null ? author.getName() : "Usuário",
                            author != null ? author.getAvatarUrl() : null
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
