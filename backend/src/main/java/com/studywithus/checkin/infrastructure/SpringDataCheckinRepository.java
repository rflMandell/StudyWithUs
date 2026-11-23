package com.studywithus.checkin.infrastructure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCheckinRepository extends JpaRepository<CheckinJpaEntity, UUID> {

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.id = :id AND c.deletedAt IS NULL")
    Optional<CheckinJpaEntity> findActiveById(@Param("id") UUID id);

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.communityId = :communityId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<CheckinJpaEntity> findActiveByCommunityId(@Param("communityId") UUID communityId, Pageable pageable);

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.userId = :userId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
    Page<CheckinJpaEntity> findActiveByUserId(@Param("userId") UUID userId, Pageable pageable);

    @Query("SELECT COUNT(c) FROM CheckinJpaEntity c WHERE c.userId = :userId AND c.deletedAt IS NULL")
    long countActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(c) > 0 FROM CheckinJpaEntity c WHERE c.userId = :userId AND c.deletedAt IS NULL AND c.createdAt >= :startInclusive AND c.createdAt < :endExclusive")
    boolean existsActiveByUserIdBetween(
            @Param("userId") UUID userId,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query("SELECT COUNT(c) > 0 FROM CheckinJpaEntity c WHERE c.userId = :userId AND c.communityId = :communityId AND c.deletedAt IS NULL AND c.createdAt >= :startInclusive AND c.createdAt < :endExclusive")
    boolean existsActiveByUserIdAndCommunityIdBetween(
            @Param("userId") UUID userId,
            @Param("communityId") UUID communityId,
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query("SELECT c.createdAt FROM CheckinJpaEntity c WHERE c.userId = :userId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<Instant> findActiveCreatedAtByUserId(@Param("userId") UUID userId);

    @Query("SELECT c.createdAt FROM CheckinJpaEntity c WHERE c.userId = :userId AND c.communityId = :communityId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
    List<Instant> findActiveCreatedAtByUserIdAndCommunityId(@Param("userId") UUID userId, @Param("communityId") UUID communityId);

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND c.communityId IN :communityIds ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByCommunities(
            @Param("communityIds") Collection<UUID> communityIds,
            Pageable pageable
    );

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND c.communityId IN :communityIds AND c.createdAt < :cursor ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByCommunitiesWithCursor(
            @Param("communityIds") Collection<UUID> communityIds,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND c.communityId IN :communityIds AND (c.createdAt < :cursorTime OR (c.createdAt = :cursorTime AND c.id < :cursorId)) ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByCommunitiesWithCursor(
            @Param("communityIds") Collection<UUID> communityIds,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND (c.communityId IN :communityIds OR (c.userId IN :followingIds AND EXISTS (SELECT 1 FROM CommunityJpaEntity comm WHERE comm.id = c.communityId AND comm.deletedAt IS NULL AND comm.visibility = com.studywithus.community.domain.CommunityVisibility.PUBLIC))) ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByCommunitiesAndFollowing(
            @Param("communityIds") Collection<UUID> communityIds,
            @Param("followingIds") Collection<UUID> followingIds,
            Pageable pageable
    );

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND (c.communityId IN :communityIds OR (c.userId IN :followingIds AND EXISTS (SELECT 1 FROM CommunityJpaEntity comm WHERE comm.id = c.communityId AND comm.deletedAt IS NULL AND comm.visibility = com.studywithus.community.domain.CommunityVisibility.PUBLIC))) AND c.createdAt < :cursor ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByCommunitiesAndFollowingWithCursor(
            @Param("communityIds") Collection<UUID> communityIds,
            @Param("followingIds") Collection<UUID> followingIds,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND (c.communityId IN :communityIds OR (c.userId IN :followingIds AND EXISTS (SELECT 1 FROM CommunityJpaEntity comm WHERE comm.id = c.communityId AND comm.deletedAt IS NULL AND comm.visibility = com.studywithus.community.domain.CommunityVisibility.PUBLIC))) AND (c.createdAt < :cursorTime OR (c.createdAt = :cursorTime AND c.id < :cursorId)) ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByCommunitiesAndFollowingWithCursor(
            @Param("communityIds") Collection<UUID> communityIds,
            @Param("followingIds") Collection<UUID> followingIds,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND c.userId IN :followingIds AND EXISTS (SELECT 1 FROM CommunityJpaEntity comm WHERE comm.id = c.communityId AND comm.deletedAt IS NULL AND comm.visibility = com.studywithus.community.domain.CommunityVisibility.PUBLIC) ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByFollowing(
            @Param("followingIds") Collection<UUID> followingIds,
            Pageable pageable
    );

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND c.userId IN :followingIds AND EXISTS (SELECT 1 FROM CommunityJpaEntity comm WHERE comm.id = c.communityId AND comm.deletedAt IS NULL AND comm.visibility = com.studywithus.community.domain.CommunityVisibility.PUBLIC) AND c.createdAt < :cursor ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByFollowingWithCursor(
            @Param("followingIds") Collection<UUID> followingIds,
            @Param("cursor") Instant cursor,
            Pageable pageable
    );

    @Query("SELECT c FROM CheckinJpaEntity c WHERE c.deletedAt IS NULL AND c.userId IN :followingIds AND EXISTS (SELECT 1 FROM CommunityJpaEntity comm WHERE comm.id = c.communityId AND comm.deletedAt IS NULL AND comm.visibility = com.studywithus.community.domain.CommunityVisibility.PUBLIC) AND (c.createdAt < :cursorTime OR (c.createdAt = :cursorTime AND c.id < :cursorId)) ORDER BY c.createdAt DESC, c.id DESC")
    List<CheckinJpaEntity> findFeedByFollowingWithCursor(
            @Param("followingIds") Collection<UUID> followingIds,
            @Param("cursorTime") Instant cursorTime,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
