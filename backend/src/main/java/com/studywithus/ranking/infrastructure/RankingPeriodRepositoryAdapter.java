package com.studywithus.ranking.infrastructure;

import com.studywithus.ranking.domain.RankingPeriod;
import com.studywithus.ranking.domain.RankingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingPeriodRepositoryAdapter implements RankingPeriodRepository {

    private final SpringDataRankingPeriodRepository repository;

    @Override
    public RankingPeriod save(RankingPeriod period) {
        RankingPeriodJpaEntity entity = RankingPeriodJpaEntity.fromDomain(period);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<RankingPeriod> findActiveByCommunityId(UUID communityId) {
        return repository.findActiveByCommunityId(communityId).map(RankingPeriodJpaEntity::toDomain);
    }

    @Override
    public Optional<RankingPeriod> findById(UUID id) {
        return repository.findById(id).map(RankingPeriodJpaEntity::toDomain);
    }
}
