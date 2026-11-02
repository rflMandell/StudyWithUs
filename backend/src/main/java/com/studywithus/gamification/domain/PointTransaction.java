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
public class PointTransaction {
    private UUID id;
    private UUID userId;
    private UUID communityId;
    private UUID checkinId;
    private UUID challengeId;
    private int amount;
    private PointTransactionType type;
    private Instant createdAt;
    private Instant reversedAt;
    private UUID reversalId;

    public static PointTransaction create(
            UUID userId,
            UUID communityId,
            UUID checkinId,
            UUID challengeId,
            int amount,
            PointTransactionType type
    ) {
        return PointTransaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .communityId(communityId)
                .checkinId(checkinId)
                .challengeId(challengeId)
                .amount(amount)
                .type(type)
                .createdAt(Instant.now())
                .build();
    }

    public static PointTransaction createReversal(PointTransaction original, int reversalAmount) {
        UUID newReversalId = UUID.randomUUID();
        return PointTransaction.builder()
                .id(newReversalId)
                .userId(original.getUserId())
                .communityId(original.getCommunityId())
                .checkinId(original.getCheckinId())
                .challengeId(original.getChallengeId())
                .amount(reversalAmount)
                .type(PointTransactionType.REVERSAL)
                .createdAt(Instant.now())
                .build();
    }

    public boolean isReversed() {
        return reversedAt != null;
    }

    public void markReversed(UUID reversalId) {
        this.reversedAt = Instant.now();
        this.reversalId = reversalId;
    }
}
