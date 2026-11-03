package com.studywithus.ranking.domain;

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
public class RankingEntry {
    private UUID rankingPeriodId;
    private UUID userId;
    private long points;
    private int position;
    private int streakAtUpdate;
    private Instant updatedAt;

    public static RankingEntry create(UUID rankingPeriodId, UUID userId, long points) {
        return RankingEntry.builder()
                .rankingPeriodId(rankingPeriodId)
                .userId(userId)
                .points(points)
                .position(0)
                .streakAtUpdate(0)
                .updatedAt(Instant.now())
                .build();
    }

    public void addPoints(long amount) {
        this.points = Math.max(0, this.points + amount);
        this.updatedAt = Instant.now();
    }

    public void updatePosition(int position) {
        this.position = position;
    }
}
