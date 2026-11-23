package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueSeason;
import com.studywithus.league.domain.LeagueSeasonStatus;
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
@Table(name = "league_seasons")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueSeasonJpaEntity {

    @Id
    private UUID id;

    @Column(name = "week_key", nullable = false, unique = true, length = 20)
    private String weekKey;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LeagueSeasonStatus status;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static LeagueSeasonJpaEntity fromDomain(LeagueSeason d) {
        return LeagueSeasonJpaEntity.builder()
                .id(d.getId())
                .weekKey(d.getWeekKey())
                .startsAt(d.getStartsAt())
                .endsAt(d.getEndsAt())
                .status(d.getStatus())
                .closedAt(d.getClosedAt())
                .createdAt(d.getCreatedAt())
                .build();
    }

    public LeagueSeason toDomain() {
        return LeagueSeason.builder()
                .id(id)
                .weekKey(weekKey)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .status(status)
                .closedAt(closedAt)
                .createdAt(createdAt)
                .build();
    }
}
