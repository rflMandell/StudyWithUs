package com.studywithus.league.application;

import com.studywithus.league.domain.LeagueLeaderboardCachePort;
import com.studywithus.league.domain.LeagueParticipant;
import com.studywithus.league.domain.LeagueRepository;
import com.studywithus.league.domain.LeagueSeason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordLeagueScoreUseCase {

    private final LeaguePoolAssignmentService assignmentService;
    private final LeagueRepository leagueRepository;
    private final LeagueLeaderboardCachePort leaderboardCachePort;

    @Transactional
    public LeagueParticipant execute(UUID userId, long deltaScore) {
        LeagueSeason season = assignmentService.getOrCreateActiveSeason();
        if (!season.isActive()) {
            return null;
        }
        LeagueParticipant participant = assignmentService.getOrAssignParticipant(season, userId);
        participant.addScore(deltaScore);
        LeagueParticipant saved = leagueRepository.saveParticipant(participant);
        leaderboardCachePort.upsertScore(saved.getPoolId(), userId, saved.getWeeklyScore());
        return saved;
    }
}
