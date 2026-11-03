package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.CommunityStreak;
import com.studywithus.gamification.domain.CommunityStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CommunityStreakRepositoryAdapter implements CommunityStreakRepository {

    private final SpringDataCommunityStreakRepository repository;

    @Override
    public CommunityStreak save(CommunityStreak streak) {
        CommunityStreakJpaEntity entity = CommunityStreakJpaEntity.fromDomain(streak);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<CommunityStreak> findByCommunityIdAndUserId(UUID communityId, UUID userId) {
        return repository.findById(new CommunityStreakId(communityId, userId))
                .map(CommunityStreakJpaEntity::toDomain);
    }
}
