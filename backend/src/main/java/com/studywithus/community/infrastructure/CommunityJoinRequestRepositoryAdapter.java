package com.studywithus.community.infrastructure;

import com.studywithus.community.domain.CommunityJoinRequest;
import com.studywithus.community.domain.CommunityJoinRequestRepository;
import com.studywithus.community.domain.CommunityJoinRequestStatus;
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
public class CommunityJoinRequestRepositoryAdapter implements CommunityJoinRequestRepository {

    private final SpringDataCommunityJoinRequestRepository repository;

    @Override
    public CommunityJoinRequest save(CommunityJoinRequest request) {
        CommunityJoinRequestJpaEntity entity = CommunityJoinRequestJpaEntity.fromDomain(request);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<CommunityJoinRequest> findById(UUID id) {
        return repository.findById(id).map(CommunityJoinRequestJpaEntity::toDomain);
    }

    @Override
    public Optional<CommunityJoinRequest> findPendingByCommunityIdAndUserId(UUID communityId, UUID userId) {
        return repository.findPendingByCommunityIdAndUserId(communityId, userId)
                .map(CommunityJoinRequestJpaEntity::toDomain);
    }

    @Override
    public PagedResult<CommunityJoinRequest> findByCommunityIdAndStatus(UUID communityId, CommunityJoinRequestStatus status, int page, int size) {
        Page<CommunityJoinRequestJpaEntity> resultPage = repository.findByCommunityIdAndStatusOrderByCreatedAtDesc(
                communityId,
                status,
                PageRequest.of(page, size)
        );

        List<CommunityJoinRequest> items = resultPage.getContent().stream()
                .map(CommunityJoinRequestJpaEntity::toDomain)
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
    public List<CommunityJoinRequest> findByCommunityIdAndStatus(UUID communityId, CommunityJoinRequestStatus status) {
        return repository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status).stream()
                .map(CommunityJoinRequestJpaEntity::toDomain)
                .toList();
    }
}
