package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.UserCosmetic;
import com.studywithus.gamification.domain.UserCosmeticRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserCosmeticRepositoryAdapter implements UserCosmeticRepository {

    private final SpringDataUserCosmeticRepository repository;

    @Override
    public UserCosmetic save(UserCosmetic cosmetic) {
        return repository.save(UserCosmeticJpaEntity.fromDomain(cosmetic)).toDomain();
    }

    @Override
    public boolean existsByUserIdAndCosmeticCode(UUID userId, String cosmeticCode) {
        return repository.existsByUserIdAndCosmeticCode(userId, cosmeticCode);
    }

    @Override
    public List<UserCosmetic> findByUserId(UUID userId) {
        return repository.findByUserIdOrderByUnlockedAtLevelAsc(userId).stream()
                .map(UserCosmeticJpaEntity::toDomain)
                .toList();
    }
}
