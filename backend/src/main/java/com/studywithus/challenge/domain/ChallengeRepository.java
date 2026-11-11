package com.studywithus.challenge.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChallengeRepository {
    Challenge save(Challenge challenge);
    Optional<Challenge> findById(UUID id);
    List<Challenge> findByCommunityId(UUID communityId);
    List<Challenge> findActiveChallengesByCommunityId(UUID communityId, Instant now);

    ChallengeParticipant saveParticipant(ChallengeParticipant participant);
    Optional<ChallengeParticipant> findParticipant(UUID challengeId, UUID userId);
    List<ChallengeParticipant> findActiveParticipationsByUserAndCommunity(UUID userId, UUID communityId, Instant now);
    List<ChallengeParticipant> findParticipantsByChallengeId(UUID challengeId);

    void saveReward(ChallengeReward reward);
    void saveAllRewards(List<ChallengeReward> rewards);
    List<ChallengeReward> findRewardsByChallengeId(UUID challengeId);
}
