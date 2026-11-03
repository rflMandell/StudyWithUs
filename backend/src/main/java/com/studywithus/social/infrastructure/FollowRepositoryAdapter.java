package com.studywithus.social.infrastructure;

import com.studywithus.social.domain.Follow;
import com.studywithus.social.domain.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FollowRepositoryAdapter implements FollowRepository {

    private final SpringDataFollowRepository repository;

    @Override
    public Follow save(Follow follow) {
        FollowJpaEntity entity = FollowJpaEntity.fromDomain(follow);
        return repository.save(entity).toDomain();
    }

    @Override
    public List<UUID> findFollowingIdsByFollowerId(UUID followerId) {
        return repository.findFollowingIdsByFollowerId(followerId);
    }

    @Override
    public boolean existsByFollowerIdAndFollowingId(UUID followerId, UUID followingId) {
        return repository.existsById(new FollowId(followerId, followingId));
    }

    @Override
    public void deleteByFollowerIdAndFollowingId(UUID followerId, UUID followingId) {
        repository.deleteById(new FollowId(followerId, followingId));
    }
}
