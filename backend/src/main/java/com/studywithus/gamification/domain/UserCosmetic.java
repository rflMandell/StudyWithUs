package com.studywithus.gamification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCosmetic {
    private UUID id;
    private UUID userId;
    private String cosmeticCode;
    private CosmeticType cosmeticType;
    private int unlockedAtLevel;
    private Instant unlockedAt;

    public static UserCosmetic create(UUID userId, String cosmeticCode, CosmeticType cosmeticType, int unlockedAtLevel) {
        return UserCosmetic.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .cosmeticCode(cosmeticCode)
                .cosmeticType(cosmeticType)
                .unlockedAtLevel(unlockedAtLevel)
                .unlockedAt(Instant.now())
                .build();
    }
}
