package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.CosmeticType;
import com.studywithus.gamification.domain.LevelCosmeticCatalog;
import com.studywithus.gamification.domain.UserCosmetic;
import com.studywithus.gamification.domain.UserLevel;

import java.util.List;
import java.util.UUID;

public record UserLevelResult(
        UUID userId,
        int level,
        long totalXp,
        long currentLevelXp,
        long nextLevelXp,
        double progressPercent,
        String activeTitle,
        String activeAvatarBorder,
        List<CosmeticItemResult> unlockedCosmetics
) {
    public record CosmeticItemResult(
            String code,
            String displayName,
            CosmeticType type,
            int unlockedAtLevel
    ) {}

    public static UserLevelResult from(UserLevel userLevel, List<UserCosmetic> persistedCosmetics) {
        List<CosmeticItemResult> cosmetics;
        if (persistedCosmetics != null && !persistedCosmetics.isEmpty()) {
            cosmetics = persistedCosmetics.stream()
                    .map(c -> {
                        String displayName = LevelCosmeticCatalog.findByCode(c.getCosmeticCode())
                                .map(LevelCosmeticCatalog.CosmeticDefinition::displayName)
                                .orElse(c.getCosmeticCode());
                        return new CosmeticItemResult(c.getCosmeticCode(), displayName, c.getCosmeticType(), c.getUnlockedAtLevel());
                    })
                    .toList();
        } else {
            cosmetics = LevelCosmeticCatalog.getUnlockedUpToLevel(userLevel.getCurrentLevel()).stream()
                    .map(d -> new CosmeticItemResult(d.code(), d.displayName(), d.type(), d.requiredLevel()))
                    .toList();
        }

        return new UserLevelResult(
                userLevel.getUserId(),
                userLevel.getCurrentLevel(),
                userLevel.getTotalXp(),
                userLevel.getCurrentLevelXp(),
                userLevel.getNextLevelXp(),
                userLevel.progressPercent(),
                userLevel.getActiveTitle(),
                userLevel.getActiveAvatarBorder(),
                cosmetics
        );
    }
}
