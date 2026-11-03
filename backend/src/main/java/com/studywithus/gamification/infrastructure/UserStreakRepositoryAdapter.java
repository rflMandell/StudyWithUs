package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.UserStreak;
import com.studywithus.gamification.domain.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserStreakRepositoryAdapter implements UserStreakRepository {

    private final SpringDataUserStreakRepository repository;

    @Override
    public UserStreak save(UserStreak streak) {
        UserStreakJpaEntity entity = UserStreakJpaEntity.fromDomain(streak);
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<UserStreak> findByUserId(UUID userId) {
        return repository.findById(userId).map(UserStreakJpaEntity::toDomain);
    }
}
