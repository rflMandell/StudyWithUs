package com.studywithus.social.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface CommentLikeRepository {
    boolean exists(UUID commentId, UUID userId);
    void save(UUID commentId, UUID userId);
    void delete(UUID commentId, UUID userId);
    long countByCommentId(UUID commentId);
    Map<UUID, Long> countByCommentIds(Collection<UUID> commentIds);
    Set<UUID> findLikedCommentIdsByUser(Collection<UUID> commentIds, UUID userId);
}
