package com.studywithus.league.application;

import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeagueLeaderboardCachePort;
import com.studywithus.league.domain.LeagueParticipant;
import com.studywithus.league.domain.LeaguePool;
import com.studywithus.league.domain.LeagueRepository;
import com.studywithus.league.domain.LeagueSeason;
import com.studywithus.league.domain.UserLeagueState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaguePoolAssignmentService {

    private final LeagueRepository leagueRepository;
    private final LeagueLeaderboardCachePort leaderboardCachePort;

    @Transactional
    public synchronized LeagueSeason getOrCreateActiveSeason() {
        return leagueRepository.findActiveSeason()
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    String weekKey = LeagueSeason.currentWeekKey(now);
                    Optional<LeagueSeason> existingByWeek = leagueRepository.findSeasonByWeekKey(weekKey);
                    if (existingByWeek.isPresent()) {
                        return existingByWeek.get();
                    }
                    return leagueRepository.saveSeason(LeagueSeason.startNewForWeek(weekKey, now));
                });
    }

    @Transactional
    public synchronized LeagueParticipant getOrAssignParticipant(LeagueSeason season, UUID userId) {
        Optional<LeagueParticipant> existing = leagueRepository.findParticipant(season.getId(), userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        UserLeagueState state = leagueRepository.findUserState(userId)
                .orElseGet(() -> leagueRepository.saveUserState(UserLeagueState.initial(userId)));

        LeagueDivision division = state.getCurrentDivision();
        LeaguePool pool = leagueRepository.findFirstPoolWithCapacity(season.getId(), division)
                .orElseGet(() -> {
                    int nextPoolNumber = leagueRepository.findMaxPoolNumber(season.getId(), division) + 1;
                    return leagueRepository.savePool(LeaguePool.create(season.getId(), division, nextPoolNumber));
                });

        pool.incrementParticipants();
        leagueRepository.savePool(pool);

        LeagueParticipant participant = leagueRepository.saveParticipant(
                LeagueParticipant.create(pool.getId(), season.getId(), userId, division)
        );
        leaderboardCachePort.upsertScore(pool.getId(), userId, participant.getWeeklyScore());
        return participant;
    }
}
