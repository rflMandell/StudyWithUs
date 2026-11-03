package com.studywithus.ranking.infrastructure;

import com.studywithus.ranking.domain.RankingEntry;
import com.studywithus.ranking.domain.RankingEntryRepository;
import com.studywithus.shared.domain.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RankingEntryRepositoryAdapter implements RankingEntryRepository {

    private final SpringDataRankingEntryRepository repository;

    @Override
    public RankingEntry save(RankingEntry entry) {
        RankingEntryJpaEntity entity = RankingEntryJpaEntity.fromDomain(entry);
        return repository.save(entity).toDomain();
    }

    @Override
    public List<RankingEntry> saveAll(List<RankingEntry> entries) {
        List<RankingEntryJpaEntity> entities = entries.stream()
                .map(RankingEntryJpaEntity::fromDomain)
                .toList();
        return repository.saveAll(entities).stream()
                .map(RankingEntryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<RankingEntry> findByPeriodIdAndUserId(UUID rankingPeriodId, UUID userId) {
        return repository.findByRankingPeriodIdAndUserId(rankingPeriodId, userId)
                .map(RankingEntryJpaEntity::toDomain);
    }

    @Override
    public List<RankingEntry> findByPeriodIdOrderByPointsDesc(UUID rankingPeriodId) {
        return repository.findByRankingPeriodIdOrderByPointsDescUpdatedAtAsc(rankingPeriodId).stream()
                .map(RankingEntryJpaEntity::toDomain)
                .toList();
    }

    @Override
    public PagedResult<RankingEntry> findByPeriodIdOrderByPositionAsc(UUID rankingPeriodId, int page, int size) {
        Page<RankingEntryJpaEntity> resultPage = repository.findByRankingPeriodIdOrderByPositionAsc(
                rankingPeriodId,
                PageRequest.of(page, size)
        );

        List<RankingEntry> items = resultPage.getContent().stream()
                .map(RankingEntryJpaEntity::toDomain)
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
}
