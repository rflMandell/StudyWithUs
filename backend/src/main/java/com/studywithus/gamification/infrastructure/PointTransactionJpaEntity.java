package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.PointTransaction;
import com.studywithus.gamification.domain.PointTransactionType;
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
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "point_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointTransactionJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "community_id")
    private UUID communityId;

    @Column(name = "checkin_id")
    private UUID checkinId;

    @Column(name = "challenge_id")
    private UUID challengeId;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PointTransactionType type;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversal_id")
    private UUID reversalId;

    public static PointTransactionJpaEntity fromDomain(PointTransaction domain) {
        return PointTransactionJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .communityId(domain.getCommunityId())
                .checkinId(domain.getCheckinId())
                .challengeId(domain.getChallengeId())
                .amount(domain.getAmount())
                .type(domain.getType())
                .createdAt(domain.getCreatedAt())
                .reversedAt(domain.getReversedAt())
                .reversalId(domain.getReversalId())
                .build();
    }

    public PointTransaction toDomain() {
        return PointTransaction.builder()
                .id(this.id)
                .userId(this.userId)
                .communityId(this.communityId)
                .checkinId(this.checkinId)
                .challengeId(this.challengeId)
                .amount(this.amount)
                .type(this.type)
                .createdAt(this.createdAt)
                .reversedAt(this.reversedAt)
                .reversalId(this.reversalId)
                .build();
    }
}
