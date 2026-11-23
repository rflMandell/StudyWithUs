package com.studywithus.checkin.infrastructure;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CheckinRepositoryAdapter implements CheckinRepository {

    private final SpringDataCheckinRepository repository;

    @Override
    public Checkin save(Checkin checkin) {
        CheckinJpaEntity entity = CheckinJpaEntity.fromDomain(checkin);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<Checkin> findById(UUID id) {
        return repository.findActiveById(id).map(CheckinJpaEntity::toDomain);
    }

    @Override
    public PagedResult<Checkin> findByCommunityIdActive(UUID communityId, int page, int size) {
        Page<CheckinJpaEntity> resultPage = repository.findActiveByCommunityId(
                communityId,
                PageRequest.of(page, size)
        );

        List<Checkin> items = resultPage.getContent().stream()
                .map(CheckinJpaEntity::toDomain)
                .toList();

        return new PagedResult<>(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.hasNext()
        );
    }

    @Override
    public PagedResult<Checkin> findByUserIdActive(UUID userId, int page, int size) {
        Page<CheckinJpaEntity> resultPage = repository.findActiveByUserId(
                userId,
                PageRequest.of(page, size)
        );

        List<Checkin> items = resultPage.getContent().stream()
                .map(CheckinJpaEntity::toDomain)
                .toList();

        return new PagedResult<>(
                items,
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages(),
                resultPage.hasNext()
        );
    }

    @Override
    public long countActiveByUserId(UUID userId) {
        return repository.countActiveByUserId(userId);
    }

    @Override
    public List<Checkin> findFeedCheckins(
            Collection<UUID> communityIds,
            Collection<UUID> followingIds,
            Instant cursor,
            int limit
    ) {
        return findFeedCheckins(communityIds, followingIds, cursor, null, limit);
    }

    @Override
    public List<Checkin> findFeedCheckins(
            Collection<UUID> communityIds,
            Collection<UUID> followingIds,
            Instant cursorTime,
            UUID cursorId,
            int limit
    ) {
        boolean hasCommunities = communityIds != null && !communityIds.isEmpty();
        boolean hasFollowing = followingIds != null && !followingIds.isEmpty();

        if (!hasCommunities && !hasFollowing) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<CheckinJpaEntity> entities;

        if (cursorTime == null) {
            if (hasCommunities && hasFollowing) {
                entities = repository.findFeedByCommunitiesAndFollowing(communityIds, followingIds, pageable);
            } else if (hasCommunities) {
                entities = repository.findFeedByCommunities(communityIds, pageable);
            } else {
                entities = repository.findFeedByFollowing(followingIds, pageable);
            }
        } else if (cursorId == null) {
            if (hasCommunities && hasFollowing) {
                entities = repository.findFeedByCommunitiesAndFollowingWithCursor(communityIds, followingIds, cursorTime, pageable);
            } else if (hasCommunities) {
                entities = repository.findFeedByCommunitiesWithCursor(communityIds, cursorTime, pageable);
            } else {
                entities = repository.findFeedByFollowingWithCursor(followingIds, cursorTime, pageable);
            }
        } else {
            if (hasCommunities && hasFollowing) {
                entities = repository.findFeedByCommunitiesAndFollowingWithCursor(communityIds, followingIds, cursorTime, cursorId, pageable);
            } else if (hasCommunities) {
                entities = repository.findFeedByCommunitiesWithCursor(communityIds, cursorTime, cursorId, pageable);
            } else {
                entities = repository.findFeedByFollowingWithCursor(followingIds, cursorTime, cursorId, pageable);
            }
        }

        return entities.stream().map(CheckinJpaEntity::toDomain).toList();
    }

    @Override
    public boolean existsActiveByUserIdBetween(UUID userId, Instant startInclusive, Instant endExclusive) {
        return repository.existsActiveByUserIdBetween(userId, startInclusive, endExclusive);
    }

    @Override
    public boolean existsActiveByUserIdAndCommunityIdBetween(UUID userId, UUID communityId, Instant startInclusive, Instant endExclusive) {
        return repository.existsActiveByUserIdAndCommunityIdBetween(userId, communityId, startInclusive, endExclusive);
    }

    @Override
    public List<Instant> findActiveCreatedAtByUserId(UUID userId) {
        return repository.findActiveCreatedAtByUserId(userId);
    }

    @Override
    public List<Instant> findActiveCreatedAtByUserIdAndCommunityId(UUID userId, UUID communityId) {
        return repository.findActiveCreatedAtByUserIdAndCommunityId(userId, communityId);
    }

    @Override
    public List<Checkin> findAllActiveByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return repository.findAllActiveByIds(ids).stream()
                .map(CheckinJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Checkin> findByIdIncludingDeleted(UUID id) {
        return repository.findByIdIncludingDeleted(id).map(CheckinJpaEntity::toDomain);
    }
}
