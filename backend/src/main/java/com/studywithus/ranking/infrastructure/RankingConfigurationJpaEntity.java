package com.studywithus.ranking.infrastructure;

import com.studywithus.ranking.domain.RankingConfiguration;
import com.studywithus.ranking.domain.RankingPeriodType;
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
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ranking_configurations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankingConfigurationJpaEntity {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false, unique = true)
    private UUID communityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private RankingPeriodType periodType;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static RankingConfigurationJpaEntity fromDomain(RankingConfiguration domain) {
        return RankingConfigurationJpaEntity.builder()
                .id(domain.getId())
                .communityId(domain.getCommunityId())
                .periodType(domain.getPeriodType())
                .timezone(domain.getTimezone())
                .startDate(domain.getStartDate())
                .endDate(domain.getEndDate())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public RankingConfiguration toDomain() {
        return RankingConfiguration.builder()
                .id(this.id)
                .communityId(this.communityId)
                .periodType(this.periodType)
                .timezone(this.timezone)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .build();
    }
}
