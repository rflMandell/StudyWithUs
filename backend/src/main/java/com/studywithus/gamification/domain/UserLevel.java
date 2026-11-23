package com.studywithus.gamification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLevel {
    private UUID userId;
    private int currentLevel;
    private long totalXp;
    private long currentLevelXp;
    private long nextLevelXp;
    private String activeTitle;
    private String activeAvatarBorder;
    private Instant updatedAt;

    public static UserLevel initial(UUID userId) {
        return UserLevel.builder()
                .userId(userId)
                .currentLevel(1)
                .totalXp(0L)
                .currentLevelXp(0L)
                .nextLevelXp(LevelCurve.xpRequiredToAdvanceFrom(1))
                .activeTitle(LevelCosmeticCatalog.getDefaultTitleForLevel(1))
                .activeAvatarBorder(LevelCosmeticCatalog.getDefaultBorderForLevel(1))
                .updatedAt(Instant.now())
                .build();
    }

    public boolean applyXp(long deltaXp) {
        if (deltaXp <= 0) {
            return false;
        }
        int previousLevel = this.currentLevel;
        this.totalXp += deltaXp;
        LevelCurve.LevelProgress progress = LevelCurve.calculate(this.totalXp);
        this.currentLevel = progress.level();
        this.currentLevelXp = progress.currentLevelXp();
        this.nextLevelXp = progress.nextLevelXp();
        this.updatedAt = Instant.now();

        if (this.currentLevel > previousLevel) {
            this.activeTitle = LevelCosmeticCatalog.getDefaultTitleForLevel(this.currentLevel);
            this.activeAvatarBorder = LevelCosmeticCatalog.getDefaultBorderForLevel(this.currentLevel);
            return true;
        }
        return false;
    }

    public double progressPercent() {
        if (nextLevelXp <= 0) {
            return 0.0;
        }
        return Math.round(((double) currentLevelXp / nextLevelXp) * 1000.0) / 10.0;
    }
}
