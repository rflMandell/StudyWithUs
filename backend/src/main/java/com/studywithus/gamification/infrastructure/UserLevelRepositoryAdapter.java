package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.UserLevel;
import com.studywithus.gamification.domain.UserLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserLevelRepositoryAdapter implements UserLevelRepository {

    private final SpringDataUserLevelRepository repository;

    @Override
    public UserLevel save(UserLevel userLevel) {
        return repository.save(UserLevelJpaEntity.fromDomain(userLevel)).toDomain();
    }

    @Override
    public Optional<UserLevel> findByUserId(UUID userId) {
        return repository.findById(userId).map(UserLevelJpaEntity::toDomain);
    }

    @Override
    public List<UserLevel> findAllByUserIds(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return repository.findByUserIdIn(userIds).stream()
                .map(UserLevelJpaEntity::toDomain)
                .toList();
    }
}
