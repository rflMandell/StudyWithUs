package com.studywithus.challenge.infrastructure;

import com.studywithus.challenge.domain.ChallengeReward;
import com.studywithus.challenge.domain.RewardType;
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

import java.util.UUID;

@Entity
@Table(name = "challenge_rewards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeRewardJpaEntity {

    @Id
    private UUID id;

    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RewardType type;

    @Column(name = "\"value\"")
    private Integer value;

    @Column(name = "badge_id")
    private UUID badgeId;

    public static ChallengeRewardJpaEntity fromDomain(ChallengeReward domain) {
        return ChallengeRewardJpaEntity.builder()
                .id(domain.getId() != null ? domain.getId() : UUID.randomUUID())
                .challengeId(domain.getChallengeId())
                .type(domain.getType())
                .value(domain.getValue())
                .badgeId(domain.getBadgeId())
                .build();
    }

    public ChallengeReward toDomain() {
        return ChallengeReward.builder()
                .id(this.id)
                .challengeId(this.challengeId)
                .type(this.type)
                .value(this.value)
                .badgeId(this.badgeId)
                .build();
    }
}
