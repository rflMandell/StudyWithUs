package com.studywithus.league.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeagueRepository {

    Optional<LeagueSeason> findActiveSeason();
    Optional<LeagueSeason> findSeasonByWeekKey(String weekKey);
    LeagueSeason saveSeason(LeagueSeason season);

    Optional<LeaguePool> findPoolById(UUID poolId);
    Optional<LeaguePool> findFirstPoolWithCapacity(UUID seasonId, LeagueDivision division);
    int findMaxPoolNumber(UUID seasonId, LeagueDivision division);
    List<LeaguePool> findPoolsBySeasonId(UUID seasonId);
    LeaguePool savePool(LeaguePool pool);

    Optional<LeagueParticipant> findParticipant(UUID seasonId, UUID userId);
    List<LeagueParticipant> findParticipantsByPoolOrdered(UUID poolId);
    List<LeagueParticipant> findParticipantsBySeasonId(UUID seasonId);
    LeagueParticipant saveParticipant(LeagueParticipant participant);

    Optional<UserLeagueState> findUserState(UUID userId);
    UserLeagueState saveUserState(UserLeagueState state);

    LeagueSeasonHistory saveHistory(LeagueSeasonHistory history);
    List<LeagueSeasonHistory> findHistoryByUserId(UUID userId);
}
