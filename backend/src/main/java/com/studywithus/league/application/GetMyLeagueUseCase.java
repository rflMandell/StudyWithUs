package com.studywithus.league.application;

import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeagueLeaderboardCachePort;
import com.studywithus.league.domain.LeagueParticipant;
import com.studywithus.league.domain.LeaguePool;
import com.studywithus.league.domain.LeagueRepository;
import com.studywithus.league.domain.LeagueSeason;
import com.studywithus.league.domain.LeagueSeasonHistory;
import com.studywithus.league.domain.UserLeagueState;
import com.studywithus.shared.domain.DomainException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetMyLeagueUseCase {

    public static final int PROMOTION_TOP_N = 5;
    public static final int RELEGATION_BOTTOM_N = 5;

    public record LeaderboardEntryResult(
            int rank,
            UUID userId,
            String username,
            String name,
            String avatarUrl,
            long weeklyScore,
            String zone
    ) {}

    public record LeagueViewResult(
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
            List<LeaderboardEntryResult> leaderboard
    ) {}

    private final LeaguePoolAssignmentService assignmentService;
    private final LeagueRepository leagueRepository;
    private final LeagueLeaderboardCachePort leaderboardCachePort;
    private final UserRepository userRepository;

    @Transactional
    public LeagueViewResult executeForUser(UUID userId) {
        LeagueSeason season = assignmentService.getOrCreateActiveSeason();
        LeagueParticipant participant = assignmentService.getOrAssignParticipant(season, userId);
        LeaguePool pool = leagueRepository.findPoolById(participant.getPoolId())
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "LEAGUE_POOL_NOT_FOUND", "League pool not found"));
        UserLeagueState state = leagueRepository.findUserState(userId)
                .orElseGet(() -> UserLeagueState.initial(userId));

        List<LeaderboardEntryResult> leaderboard = buildPoolLeaderboard(pool.getId(), pool.getDivision());
        LeaderboardEntryResult me = leaderboard.stream()
                .filter(e -> e.userId().equals(userId))
                .findFirst()
                .orElse(null);

        int size = leaderboard.size();
        return new LeagueViewResult(
                season.getId(),
                season.getWeekKey(),
                pool.getId(),
                pool.getPoolNumber(),
                state.getCurrentDivision(),
                state.getHighestDivision(),
                me != null ? me.rank() : 0,
                me != null ? me.weeklyScore() : participant.getWeeklyScore(),
                me != null ? me.zone() : "SAFE",
                size,
                computePromoteCount(size, pool.getDivision()),
                computeRelegateCount(size, pool.getDivision()),
                leaderboard
        );
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResult> executeForPool(UUID poolId) {
        LeaguePool pool = leagueRepository.findPoolById(poolId)
                .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "LEAGUE_POOL_NOT_FOUND", "League pool not found"));
        return buildPoolLeaderboard(pool.getId(), pool.getDivision());
    }

    @Transactional(readOnly = true)
    public List<LeagueSeasonHistory> getHistory(UUID userId) {
        return leagueRepository.findHistoryByUserId(userId);
    }

    private List<LeaderboardEntryResult> buildPoolLeaderboard(UUID poolId, LeagueDivision division) {
        List<LeagueLeaderboardCachePort.LeaderboardScoreEntry> orderedScores;

        Optional<List<LeagueLeaderboardCachePort.LeaderboardScoreEntry>> cached = leaderboardCachePort.getPoolLeaderboard(poolId);
        if (cached.isPresent() && !cached.get().isEmpty()) {
            orderedScores = cached.get();
        } else {
            List<LeagueParticipant> dbParticipants = leagueRepository.findParticipantsByPoolOrdered(poolId);
            orderedScores = dbParticipants.stream()
                    .map(p -> new LeagueLeaderboardCachePort.LeaderboardScoreEntry(p.getUserId(), p.getWeeklyScore()))
                    .toList();
            leaderboardCachePort.warmPoolLeaderboard(poolId, orderedScores);
        }

        Set<UUID> userIds = orderedScores.stream()
                .map(LeagueLeaderboardCachePort.LeaderboardScoreEntry::userId)
                .collect(Collectors.toSet());
        Map<UUID, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        int total = orderedScores.size();
        int promoteCount = computePromoteCount(total, division);
        int relegateCount = computeRelegateCount(total, division);

        List<LeaderboardEntryResult> result = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            LeagueLeaderboardCachePort.LeaderboardScoreEntry entry = orderedScores.get(i);
            int rank = i + 1;
            String zone = resolveZone(rank, total, promoteCount, relegateCount);
            User user = users.get(entry.userId());
            result.add(new LeaderboardEntryResult(
                    rank,
                    entry.userId(),
                    user != null ? user.getUsername() : "usuario",
                    user != null ? user.getName() : "Usuário",
                    user != null ? user.getAvatarUrl() : null,
                    entry.score(),
                    zone
            ));
        }
        return result;
    }

    public static int computePromoteCount(int poolSize, LeagueDivision division) {
        if (division == LeagueDivision.MASTER || poolSize < 2) {
            return 0;
        }
        if (poolSize >= PROMOTION_TOP_N + RELEGATION_BOTTOM_N) {
            return PROMOTION_TOP_N;
        }
        return Math.min(PROMOTION_TOP_N, Math.max(1, poolSize / 3));
    }

    public static int computeRelegateCount(int poolSize, LeagueDivision division) {
        if (division == LeagueDivision.BRONZE || poolSize < 4) {
            return 0;
        }
        if (poolSize >= PROMOTION_TOP_N + RELEGATION_BOTTOM_N) {
            return RELEGATION_BOTTOM_N;
        }
        return Math.min(RELEGATION_BOTTOM_N, Math.max(1, poolSize / 3));
    }

    private String resolveZone(int rank, int total, int promoteCount, int relegateCount) {
        if (promoteCount > 0 && rank <= promoteCount) {
            return "PROMOTION";
        }
        if (relegateCount > 0 && rank > total - relegateCount) {
            return "RELEGATION";
        }
        return "SAFE";
    }
}
