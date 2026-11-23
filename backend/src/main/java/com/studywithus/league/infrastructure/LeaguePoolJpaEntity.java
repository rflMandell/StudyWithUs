package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeaguePool;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "league_pools")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaguePoolJpaEntity {

    @Id
    private UUID id;

    @Column(name = "season_id", nullable = false)
    private UUID seasonId;

    @Enumerated(EnumType.STRING)
    @Column(name = "division", nullable = false, length = 20)
    private LeagueDivision division;

    @Column(name = "pool_number", nullable = false)
    private int poolNumber;

    @Column(name = "participants_count", nullable = false)
    private int participantsCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static LeaguePoolJpaEntity fromDomain(LeaguePool d) {
        return LeaguePoolJpaEntity.builder()
                .id(d.getId())
                .seasonId(d.getSeasonId())
                .division(d.getDivision())
                .poolNumber(d.getPoolNumber())
                .participantsCount(d.getParticipantsCount())
                .createdAt(d.getCreatedAt())
                .build();
    }

    public LeaguePool toDomain() {
        return LeaguePool.builder()
                .id(id)
                .seasonId(seasonId)
                .division(division)
                .poolNumber(poolNumber)
                .participantsCount(participantsCount)
                .createdAt(createdAt)
                .build();
    }
}
