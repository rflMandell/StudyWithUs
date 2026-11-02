package com.studywithus.checkin.infrastructure;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinRepository;
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
}
