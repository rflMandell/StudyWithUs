package com.studywithus.league.application;

import com.studywithus.gamification.application.AwardBadgeCommand;
import com.studywithus.gamification.application.AwardBadgeUseCase;
import com.studywithus.gamification.domain.BadgeConstants;
import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeagueLeaderboardCachePort;
import com.studywithus.league.domain.LeagueOutcome;
import com.studywithus.league.domain.LeagueParticipant;
import com.studywithus.league.domain.LeaguePool;
import com.studywithus.league.domain.LeagueRepository;
import com.studywithus.league.domain.LeagueSeason;
import com.studywithus.league.domain.LeagueSeasonHistory;
import com.studywithus.league.domain.UserLeagueState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloseLeagueSeasonUseCase {

    public record SeasonCloseSummary(
            UUID closedSeasonId,
            String closedWeekKey,
            UUID nextSeasonId,
            String nextWeekKey,
            int totalParticipantsProcessed,
            int promotedCount,
            int relegatedCount
    ) {}

    private final LeagueRepository leagueRepository;
    private final LeaguePoolAssignmentService assignmentService;
    private final LeagueLeaderboardCachePort leaderboardCachePort;
    private final AwardBadgeUseCase awardBadgeUseCase;

    @Transactional
    public SeasonCloseSummary execute() {
        LeagueSeason activeSeason = leagueRepository.findActiveSeason()
                .orElse(null);

        if (activeSeason == null) {
            LeagueSeason newSeason = assignmentService.getOrCreateActiveSeason();
            return new SeasonCloseSummary(null, null, newSeason.getId(), newSeason.getWeekKey(), 0, 0, 0);
        }

        Instant now = Instant.now();
        activeSeason.close(now);
        leagueRepository.saveSeason(activeSeason);

        List<LeaguePool> pools = leagueRepository.findPoolsBySeasonId(activeSeason.getId());
        List<UUID> participantsToCarryOver = new ArrayList<>();
        int totalProcessed = 0;
        int promoted = 0;
        int relegated = 0;

        for (LeaguePool pool : pools) {
            List<LeagueParticipant> ranked = leagueRepository.findParticipantsByPoolOrdered(pool.getId());
            int total = ranked.size();
            int promoteCount = GetMyLeagueUseCase.computePromoteCount(total, pool.getDivision());
            int relegateCount = GetMyLeagueUseCase.computeRelegateCount(total, pool.getDivision());

            for (int i = 0; i < total; i++) {
                LeagueParticipant participant = ranked.get(i);
                int rank = i + 1;
                LeagueOutcome outcome;
                LeagueDivision nextDivision;

                if (promoteCount > 0 && rank <= promoteCount) {
                    outcome = LeagueOutcome.PROMOTED;
                    nextDivision = participant.getDivision().promote();
                    promoted++;
                } else if (relegateCount > 0 && rank > total - relegateCount) {
                    outcome = LeagueOutcome.RELEGATED;
                    nextDivision = participant.getDivision().relegate();
                    relegated++;
                } else {
                    outcome = LeagueOutcome.STAYED;
                    nextDivision = participant.getDivision();
                }

                participant.finalizeSeason(rank, outcome);
                leagueRepository.saveParticipant(participant);

                UserLeagueState state = leagueRepository.findUserState(participant.getUserId())
                        .orElseGet(() -> UserLeagueState.initial(participant.getUserId()));
                state.transitionTo(nextDivision);
                leagueRepository.saveUserState(state);

                leagueRepository.saveHistory(LeagueSeasonHistory.create(
                        activeSeason, participant, nextDivision, rank, outcome
                ));

                awardLeagueBadges(participant.getUserId(), rank, nextDivision);
                participantsToCarryOver.add(participant.getUserId());
                totalProcessed++;
            }

            leaderboardCachePort.invalidatePool(pool.getId());
        }

        String nextWeekKey = resolveNextWeekKey(activeSeason);
        LeagueSeason nextSeason = leagueRepository.findSeasonByWeekKey(nextWeekKey)
                .orElseGet(() -> leagueRepository.saveSeason(
                        LeagueSeason.startNewForWeek(nextWeekKey, activeSeason.getEndsAt().plusSeconds(60))
                ));

        // Open fresh balanced 30-participant pools for the new week
        for (UUID userId : participantsToCarryOver) {
            assignmentService.getOrAssignParticipant(nextSeason, userId);
        }

        log.info("Closed league season {} ({}): processed={}, promoted={}, relegated={}, opened={}",
                activeSeason.getWeekKey(), activeSeason.getId(), totalProcessed, promoted, relegated, nextSeason.getWeekKey());

        return new SeasonCloseSummary(
                activeSeason.getId(),
                activeSeason.getWeekKey(),
                nextSeason.getId(),
                nextSeason.getWeekKey(),
                totalProcessed,
                promoted,
                relegated
        );
    }

    private String resolveNextWeekKey(LeagueSeason closedSeason) {
        String candidate = LeagueSeason.currentWeekKey(closedSeason.getEndsAt().plus(1, ChronoUnit.HOURS));
        if (candidate.equals(closedSeason.getWeekKey())) {
            return LeagueSeason.currentWeekKey(closedSeason.getEndsAt().plus(7, ChronoUnit.DAYS));
        }
        return candidate;
    }

    private void awardLeagueBadges(UUID userId, int rank, LeagueDivision nextDivision) {
        try {
            if (rank == 1) {
                awardBadgeUseCase.execute(new AwardBadgeCommand(userId, BadgeConstants.BADGE_LEAGUE_CHAMPION_ID));
            }
            if (nextDivision == LeagueDivision.GOLD) {
                awardBadgeUseCase.execute(new AwardBadgeCommand(userId, BadgeConstants.BADGE_LEAGUE_GOLD_ID));
            } else if (nextDivision == LeagueDivision.DIAMOND) {
                awardBadgeUseCase.execute(new AwardBadgeCommand(userId, BadgeConstants.BADGE_LEAGUE_DIAMOND_ID));
            } else if (nextDivision == LeagueDivision.MASTER) {
                awardBadgeUseCase.execute(new AwardBadgeCommand(userId, BadgeConstants.BADGE_LEAGUE_MASTER_ID));
            }
        } catch (Exception e) {
            log.warn("Failed to award league badge to userId={}: {}", userId, e.getMessage());
        }
    }
}
