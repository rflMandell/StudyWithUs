package com.studywithus.social.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCommentRepository extends JpaRepository<CommentJpaEntity, UUID> {

    @Query("SELECT c FROM CommentJpaEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CommentJpaEntity> findActiveById(@Param("id") UUID id);

    @Query("SELECT c FROM CommentJpaEntity c WHERE c.checkinId = :checkinId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    Page<CommentJpaEntity> findActiveByCheckinId(@Param("checkinId") UUID checkinId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM CommentJpaEntity c WHERE c.checkinId = :checkinId AND c.deletedAt IS NULL")
    long countActiveByCheckinId(@Param("checkinId") UUID checkinId);

    @Query("SELECT c.checkinId, COUNT(c) FROM CommentJpaEntity c WHERE c.checkinId IN :checkinIds AND c.deletedAt IS NULL GROUP BY c.checkinId")
    List<Object[]> countActiveGroupedByCheckinIds(@Param("checkinIds") Collection<UUID> checkinIds);
}
