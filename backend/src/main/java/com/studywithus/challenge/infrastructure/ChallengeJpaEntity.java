package com.studywithus.challenge.infrastructure;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeScope;
import com.studywithus.challenge.domain.ChallengeStatus;
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
import java.util.ArrayList;
import java.util.UUID;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "creator_id", nullable = false)
    private UUID creatorId;

    @Column(name = "community_id")
    private UUID communityId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChallengeMetric metric;

    @Column(name = "target_value", nullable = false)
    private int targetValue;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChallengeStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ChallengeJpaEntity fromDomain(Challenge domain) {
        return ChallengeJpaEntity.builder()
                .id(domain.getId())
                .creatorId(domain.getCreatorId())
                .communityId(domain.getCommunityId())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .scope(domain.getScope())
                .metric(domain.getMetric())
                .targetValue(domain.getTargetValue())
                .startAt(domain.getStartAt())
                .endAt(domain.getEndAt())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : Instant.now())
                .updatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : Instant.now())
                .build();
    }

    public Challenge toDomain() {
        return Challenge.builder()
                .id(this.id)
                .creatorId(this.creatorId)
                .communityId(this.communityId)
                .title(this.title)
                .description(this.description)
                .scope(this.scope)
                .metric(this.metric)
                .targetValue(this.targetValue)
                .startAt(this.startAt)
                .endAt(this.endAt)
                .status(this.status)
                .rewards(new ArrayList<>())
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
