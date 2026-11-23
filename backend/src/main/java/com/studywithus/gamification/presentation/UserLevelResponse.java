package com.studywithus.gamification.presentation;

import com.studywithus.gamification.application.UserLevelResult;
import com.studywithus.gamification.domain.CosmeticType;

import java.util.List;
import java.util.UUID;

public record UserLevelResponse(
        UUID userId,
        int level,
        long totalXp,
        long currentLevelXp,
        long nextLevelXp,
        double progressPercent,
        String activeTitle,
        String activeAvatarBorder,
        List<CosmeticItemResponse> unlockedCosmetics
) {
    public record CosmeticItemResponse(
            String code,
            String displayName,
            CosmeticType type,
            int unlockedAtLevel
    ) {}

    public static UserLevelResponse from(UserLevelResult result) {
        List<CosmeticItemResponse> cosmetics = result.unlockedCosmetics() != null
                ? result.unlockedCosmetics().stream()
                        .map(c -> new CosmeticItemResponse(c.code(), c.displayName(), c.type(), c.unlockedAtLevel()))
                        .toList()
                : List.of();

        return new UserLevelResponse(
                result.userId(),
                result.level(),
                result.totalXp(),
                result.currentLevelXp(),
                result.nextLevelXp(),
                result.progressPercent(),
                result.activeTitle(),
                result.activeAvatarBorder(),
                cosmetics
        );
    }
}
