package com.studywithus.league.infrastructure;

import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeagueParticipant;
import com.studywithus.league.domain.LeaguePool;
import com.studywithus.league.domain.LeagueRepository;
import com.studywithus.league.domain.LeagueSeason;
import com.studywithus.league.domain.LeagueSeasonHistory;
import com.studywithus.league.domain.LeagueSeasonStatus;
import com.studywithus.league.domain.UserLeagueState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LeagueRepositoryAdapter implements LeagueRepository {

    private final SpringDataLeagueSeasonRepository seasonRepo;
    private final SpringDataLeaguePoolRepository poolRepo;
    private final SpringDataLeagueParticipantRepository participantRepo;
    private final SpringDataUserLeagueStateRepository stateRepo;
    private final SpringDataLeagueSeasonHistoryRepository historyRepo;

    @Override
    public Optional<LeagueSeason> findActiveSeason() {
        return seasonRepo.findFirstByStatusOrderByCreatedAtDesc(LeagueSeasonStatus.ACTIVE)
                .map(LeagueSeasonJpaEntity::toDomain);
    }

    @Override
    public Optional<LeagueSeason> findSeasonByWeekKey(String weekKey) {
        return seasonRepo.findByWeekKey(weekKey).map(LeagueSeasonJpaEntity::toDomain);
    }

    @Override
    public LeagueSeason saveSeason(LeagueSeason season) {
        return seasonRepo.save(LeagueSeasonJpaEntity.fromDomain(season)).toDomain();
    }

    @Override
    public Optional<LeaguePool> findPoolById(UUID poolId) {
        return poolRepo.findById(poolId).map(LeaguePoolJpaEntity::toDomain);
    }

    @Override
    public Optional<LeaguePool> findFirstPoolWithCapacity(UUID seasonId, LeagueDivision division) {
        return poolRepo.findFirstBySeasonIdAndDivisionAndParticipantsCountLessThanOrderByPoolNumberAsc(
                        seasonId, division, LeaguePool.MAX_PARTICIPANTS)
                .map(LeaguePoolJpaEntity::toDomain);
    }

    @Override
    public int findMaxPoolNumber(UUID seasonId, LeagueDivision division) {
        return poolRepo.findMaxPoolNumber(seasonId, division);
    }

    @Override
    public List<LeaguePool> findPoolsBySeasonId(UUID seasonId) {
        return poolRepo.findBySeasonIdOrderByDivisionAscPoolNumberAsc(seasonId).stream()
                .map(LeaguePoolJpaEntity::toDomain)
                .toList();
    }

    @Override
    public LeaguePool savePool(LeaguePool pool) {
        return poolRepo.save(LeaguePoolJpaEntity.fromDomain(pool)).toDomain();
    }

    @Override
    public Optional<LeagueParticipant> findParticipant(UUID seasonId, UUID userId) {
        return participantRepo.findBySeasonIdAndUserId(seasonId, userId)
                .map(LeagueParticipantJpaEntity::toDomain);
    }

    @Override
    public List<LeagueParticipant> findParticipantsByPoolOrdered(UUID poolId) {
        return participantRepo.findByPoolIdOrderByWeeklyScoreDescJoinedAtAsc(poolId).stream()
                .map(LeagueParticipantJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<LeagueParticipant> findParticipantsBySeasonId(UUID seasonId) {
        return participantRepo.findBySeasonId(seasonId).stream()
                .map(LeagueParticipantJpaEntity::toDomain)
                .toList();
    }

    @Override
    public LeagueParticipant saveParticipant(LeagueParticipant participant) {
        return participantRepo.save(LeagueParticipantJpaEntity.fromDomain(participant)).toDomain();
    }

    @Override
    public Optional<UserLeagueState> findUserState(UUID userId) {
        return stateRepo.findById(userId).map(UserLeagueStateJpaEntity::toDomain);
    }

    @Override
    public UserLeagueState saveUserState(UserLeagueState state) {
        return stateRepo.save(UserLeagueStateJpaEntity.fromDomain(state)).toDomain();
    }

    @Override
    public LeagueSeasonHistory saveHistory(LeagueSeasonHistory history) {
        return historyRepo.save(LeagueSeasonHistoryJpaEntity.fromDomain(history)).toDomain();
    }

    @Override
    public List<LeagueSeasonHistory> findHistoryByUserId(UUID userId) {
        return historyRepo.findByUserIdOrderByRecordedAtDesc(userId).stream()
                .map(LeagueSeasonHistoryJpaEntity::toDomain)
                .toList();
    }
}
