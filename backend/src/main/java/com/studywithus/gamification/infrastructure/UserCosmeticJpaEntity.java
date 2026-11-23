package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.CosmeticType;
import com.studywithus.gamification.domain.UserCosmetic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_cosmetics")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCosmeticJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cosmetic_code", nullable = false, length = 100)
    private String cosmeticCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "cosmetic_type", nullable = false, length = 30)
    private CosmeticType cosmeticType;

    @Column(name = "unlocked_at_level", nullable = false)
    private int unlockedAtLevel;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    public static UserCosmeticJpaEntity fromDomain(UserCosmetic domain) {
        return UserCosmeticJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .cosmeticCode(domain.getCosmeticCode())
                .cosmeticType(domain.getCosmeticType())
                .unlockedAtLevel(domain.getUnlockedAtLevel())
                .unlockedAt(domain.getUnlockedAt())
                .build();
    }

    public UserCosmetic toDomain() {
        return UserCosmetic.builder()
                .id(id)
                .userId(userId)
                .cosmeticCode(cosmeticCode)
                .cosmeticType(cosmeticType)
                .unlockedAtLevel(unlockedAtLevel)
                .unlockedAt(unlockedAt)
                .build();
    }
}
