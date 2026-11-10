package com.studywithus.social.domain;

import com.studywithus.shared.domain.PagedResult;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepository {
    Comment save(Comment comment);
    Optional<Comment> findById(UUID id);
    PagedResult<Comment> findByCheckinId(UUID checkinId, int page, int size);
    long countByCheckinId(UUID checkinId);
    Map<UUID, Long> countActiveByCheckinIds(Collection<UUID> checkinIds);
}
