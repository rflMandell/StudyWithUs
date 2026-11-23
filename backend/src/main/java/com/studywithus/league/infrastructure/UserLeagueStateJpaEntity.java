package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.UserLeagueState;
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
@Table(name = "user_league_states")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLeagueStateJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_division", nullable = false, length = 20)
    private LeagueDivision currentDivision;

    @Enumerated(EnumType.STRING)
    @Column(name = "highest_division", nullable = false, length = 20)
    private LeagueDivision highestDivision;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static UserLeagueStateJpaEntity fromDomain(UserLeagueState d) {
        return UserLeagueStateJpaEntity.builder()
                .userId(d.getUserId())
                .currentDivision(d.getCurrentDivision())
                .highestDivision(d.getHighestDivision())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    public UserLeagueState toDomain() {
        return UserLeagueState.builder()
                .userId(userId)
                .currentDivision(currentDivision)
                .highestDivision(highestDivision)
                .updatedAt(updatedAt)
                .build();
    }
}
