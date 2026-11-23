package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.UserLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_levels")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLevelJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "current_level", nullable = false)
    private int currentLevel;

    @Column(name = "total_xp", nullable = false)
    private long totalXp;

    @Column(name = "current_level_xp", nullable = false)
    private long currentLevelXp;

    @Column(name = "next_level_xp", nullable = false)
    private long nextLevelXp;

    @Column(name = "active_title", nullable = false, length = 100)
    private String activeTitle;

    @Column(name = "active_avatar_border", nullable = false, length = 100)
    private String activeAvatarBorder;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static UserLevelJpaEntity fromDomain(UserLevel domain) {
        return UserLevelJpaEntity.builder()
                .userId(domain.getUserId())
                .currentLevel(domain.getCurrentLevel())
                .totalXp(domain.getTotalXp())
                .currentLevelXp(domain.getCurrentLevelXp())
                .nextLevelXp(domain.getNextLevelXp())
                .activeTitle(domain.getActiveTitle())
                .activeAvatarBorder(domain.getActiveAvatarBorder())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public UserLevel toDomain() {
        return UserLevel.builder()
                .userId(userId)
                .currentLevel(currentLevel)
                .totalXp(totalXp)
                .currentLevelXp(currentLevelXp)
                .nextLevelXp(nextLevelXp)
                .activeTitle(activeTitle)
                .activeAvatarBorder(activeAvatarBorder)
                .updatedAt(updatedAt)
                .build();
    }
}
