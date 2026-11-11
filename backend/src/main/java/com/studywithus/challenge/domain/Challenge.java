package com.studywithus.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {
    private UUID id;
    private UUID creatorId;
    private UUID communityId;
    private String title;
    private String description;
    @Builder.Default
    private ChallengeScope scope = ChallengeScope.INDIVIDUAL;
    @Builder.Default
    private ChallengeMetric metric = ChallengeMetric.CHECKINS;
    private int targetValue;
    private Instant startAt;
    private Instant endAt;
    @Builder.Default
    private ChallengeStatus status = ChallengeStatus.ACTIVE;
    @Builder.Default
    private List<ChallengeReward> rewards = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isActive(Instant now) {
        return status == ChallengeStatus.ACTIVE
                && (startAt == null || !now.isBefore(startAt))
                && (endAt == null || !now.isAfter(endAt));
    }

    public void complete() {
        this.status = ChallengeStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = ChallengeStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
