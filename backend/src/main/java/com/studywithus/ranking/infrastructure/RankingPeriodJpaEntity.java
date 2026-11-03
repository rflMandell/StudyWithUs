package com.studywithus.ranking.infrastructure;

import com.studywithus.ranking.domain.RankingPeriod;
import com.studywithus.ranking.domain.RankingPeriodStatus;
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
@Table(name = "ranking_periods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingPeriodJpaEntity {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "configuration_id")
    private UUID configurationId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RankingPeriodStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static RankingPeriodJpaEntity fromDomain(RankingPeriod domain) {
        return RankingPeriodJpaEntity.builder()
                .id(domain.getId())
                .communityId(domain.getCommunityId())
                .configurationId(domain.getConfigurationId())
                .startsAt(domain.getStartsAt())
                .endsAt(domain.getEndsAt())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public RankingPeriod toDomain() {
        return RankingPeriod.builder()
                .id(this.id)
                .communityId(this.communityId)
                .configurationId(this.configurationId)
                .startsAt(this.startsAt)
                .endsAt(this.endsAt)
                .status(this.status)
                .createdAt(this.createdAt)
                .build();
    }
}
