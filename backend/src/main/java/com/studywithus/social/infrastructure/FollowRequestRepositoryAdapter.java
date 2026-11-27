package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.FollowRequest;
import com.studywithus.social.domain.FollowRequestRepository;
import com.studywithus.social.domain.FollowRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FollowRequestRepositoryAdapter implements FollowRequestRepository {

    private final SpringDataFollowRequestRepository repository;

    @Override
    public FollowRequest save(FollowRequest followRequest) {
        FollowRequestJpaEntity entity = FollowRequestJpaEntity.fromDomain(followRequest);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<FollowRequest> findById(UUID id) {
        return repository.findById(id).map(FollowRequestJpaEntity::toDomain);
    }

    @Override
    public Optional<FollowRequest> findByRequesterIdAndTargetId(UUID requesterId, UUID targetId) {
        return repository.findByRequesterIdAndTargetId(requesterId, targetId)
                .map(FollowRequestJpaEntity::toDomain);
    }

    @Override
    public List<FollowRequest> findPendingByTargetId(UUID targetId) {
        return repository.findByTargetIdAndStatusOrderByCreatedAtDesc(targetId, FollowRequestStatus.PENDING).stream()
                .map(FollowRequestJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByRequesterIdAndTargetId(UUID requesterId, UUID targetId) {
        repository.deleteByRequesterIdAndTargetId(requesterId, targetId);
    }
}
