package com.studywithus.league.presentation;

import com.studywithus.identity.infrastructure.SecurityUserPrincipal;
import com.studywithus.league.application.GetMyLeagueUseCase;
import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeagueOutcome;
import com.studywithus.league.domain.LeaguePool;
import com.studywithus.league.domain.LeagueSeasonHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private final GetMyLeagueUseCase getMyLeagueUseCase;

    public record DivisionInfoResponse(
            LeagueDivision division,
            int tier,
            String displayName,
            int maxPoolSize,
            int promoteTopN,
            int relegateBottomN
    ) {}

    public record LeaderboardEntryResponse(
            int rank,
            UUID userId,
            String username,
            String name,
            String avatarUrl,
            long weeklyScore,
            String zone
    ) {
        static LeaderboardEntryResponse from(GetMyLeagueUseCase.LeaderboardEntryResult r) {
            return new LeaderboardEntryResponse(r.rank(), r.userId(), r.username(), r.name(), r.avatarUrl(), r.weeklyScore(), r.zone());
        }
    }

    public record LeagueViewResponse(
            UUID seasonId,
            String weekKey,
            UUID poolId,
            int poolNumber,
            LeagueDivision currentDivision,
            LeagueDivision highestDivision,
            int myRank,
            long myWeeklyScore,
            String myZone,
            int poolSize,
            int promoteCount,
            int relegateCount,
            List<LeaderboardEntryResponse> leaderboard
    ) {
        static LeagueViewResponse from(GetMyLeagueUseCase.LeagueViewResult r) {
            return new LeagueViewResponse(
                    r.seasonId(),
                    r.weekKey(),
                    r.poolId(),
                    r.poolNumber(),
                    r.currentDivision(),
                    r.highestDivision(),
                    r.myRank(),
                    r.myWeeklyScore(),
                    r.myZone(),
                    r.poolSize(),
                    r.promoteCount(),
                    r.relegateCount(),
                    r.leaderboard().stream().map(LeaderboardEntryResponse::from).toList()
            );
        }
    }

    public record LeagueHistoryResponse(
            UUID id,
            UUID seasonId,
            String weekKey,
            UUID poolId,
            LeagueDivision division,
            LeagueDivision nextDivision,
            int finalRank,
            long weeklyScore,
            LeagueOutcome outcome,
            Instant recordedAt
    ) {
        static LeagueHistoryResponse from(LeagueSeasonHistory h) {
            return new LeagueHistoryResponse(
                    h.getId(), h.getSeasonId(), h.getWeekKey(), h.getPoolId(),
                    h.getDivision(), h.getNextDivision(), h.getFinalRank(),
                    h.getWeeklyScore(), h.getOutcome(), h.getRecordedAt()
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<LeagueViewResponse> getMyLeague(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(LeagueViewResponse.from(getMyLeagueUseCase.executeForUser(principal.getId())));
    }

    @GetMapping("/pools/{poolId}")
    public ResponseEntity<List<LeaderboardEntryResponse>> getPoolLeaderboard(@PathVariable UUID poolId) {
        return ResponseEntity.ok(
                getMyLeagueUseCase.executeForPool(poolId).stream().map(LeaderboardEntryResponse::from).toList()
        );
    }

    @GetMapping("/me/history")
    public ResponseEntity<List<LeagueHistoryResponse>> getMyHistory(
            @AuthenticationPrincipal SecurityUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                getMyLeagueUseCase.getHistory(principal.getId()).stream().map(LeagueHistoryResponse::from).toList()
        );
    }

    @GetMapping("/divisions")
    public ResponseEntity<List<DivisionInfoResponse>> listDivisions() {
        List<DivisionInfoResponse> divisions = Arrays.stream(LeagueDivision.values())
                .map(d -> new DivisionInfoResponse(
                        d,
                        d.getTier(),
                        d.getDisplayName(),
                        LeaguePool.MAX_PARTICIPANTS,
                        d == LeagueDivision.MASTER ? 0 : GetMyLeagueUseCase.PROMOTION_TOP_N,
                        d == LeagueDivision.BRONZE ? 0 : GetMyLeagueUseCase.RELEGATION_BOTTOM_N
                ))
                .toList();
        return ResponseEntity.ok(divisions);
    }
}
