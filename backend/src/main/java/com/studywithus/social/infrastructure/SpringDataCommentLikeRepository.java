package com.studywithus.social.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SpringDataCommentLikeRepository extends JpaRepository<CommentLikeJpaEntity, CommentLikeId> {

    @Query("SELECT COUNT(l) FROM CommentLikeJpaEntity l WHERE l.id.commentId = :commentId")
    long countByCommentId(@Param("commentId") UUID commentId);

    @Query("SELECT l.id.commentId, COUNT(l) FROM CommentLikeJpaEntity l WHERE l.id.commentId IN :commentIds GROUP BY l.id.commentId")
    List<Object[]> countGroupedByCommentIds(@Param("commentIds") Collection<UUID> commentIds);

    @Query("SELECT l.id.commentId FROM CommentLikeJpaEntity l WHERE l.id.commentId IN :commentIds AND l.id.userId = :userId")
    Set<UUID> findLikedCommentIdsByUser(@Param("commentIds") Collection<UUID> commentIds, @Param("userId") UUID userId);
}
