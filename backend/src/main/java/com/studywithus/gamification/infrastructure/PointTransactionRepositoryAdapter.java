package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionRepository;
import com.studywithus.gamification.domain.PointTransactionType;
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
public class PointTransactionRepositoryAdapter implements PointTransactionRepository {

    private final SpringDataPointTransactionRepository repository;

    @Override
    public PointTransaction save(PointTransaction transaction) {
        PointTransactionJpaEntity entity = PointTransactionJpaEntity.fromDomain(transaction);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<PointTransaction> findById(UUID id) {
        return repository.findById(id).map(PointTransactionJpaEntity::toDomain);
    }

    @Override
    public Optional<PointTransaction> findByCheckinIdAndType(UUID checkinId, PointTransactionType type) {
        return repository.findByCheckinIdAndType(checkinId, type).map(PointTransactionJpaEntity::toDomain);
    }

    @Override
    public PagedResult<PointTransaction> findByUserId(UUID userId, int page, int size) {
        Page<PointTransactionJpaEntity> resultPage = repository.findByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(page, size)
        );

        List<PointTransaction> items = resultPage.getContent().stream()
                .map(PointTransactionJpaEntity::toDomain)
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
    public long calculateTotalPointsByUser(UUID userId) {
        return repository.sumAmountByUserId(userId);
    }

    @Override
    public long calculateTotalPointsByUserAndCommunity(UUID userId, UUID communityId) {
        return repository.sumAmountByUserIdAndCommunityId(userId, communityId);
    }
}
