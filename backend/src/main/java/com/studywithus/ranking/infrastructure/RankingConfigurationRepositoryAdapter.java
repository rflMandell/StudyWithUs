package com.studywithus.ranking.infrastructure;

import com.studywithus.ranking.domain.RankingConfiguration;
import com.studywithus.ranking.domain.RankingConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingConfigurationRepositoryAdapter implements RankingConfigurationRepository {

    private final SpringDataRankingConfigurationRepository repository;

    @Override
    public RankingConfiguration save(RankingConfiguration config) {
        RankingConfigurationJpaEntity entity = RankingConfigurationJpaEntity.fromDomain(config);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<RankingConfiguration> findByCommunityId(UUID communityId) {
        return repository.findByCommunityId(communityId).map(RankingConfigurationJpaEntity::toDomain);
    }
}
