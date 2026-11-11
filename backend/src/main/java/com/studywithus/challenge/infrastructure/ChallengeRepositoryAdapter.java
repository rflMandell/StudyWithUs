package com.studywithus.challenge.infrastructure;

import com.studywithus.challenge.domain.Challenge;
import com.studywithus.challenge.domain.ChallengeParticipant;
import com.studywithus.challenge.domain.ChallengeRepository;
import com.studywithus.challenge.domain.ChallengeReward;
import com.studywithus.challenge.domain.ChallengeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChallengeRepositoryAdapter implements ChallengeRepository {

    private final SpringDataChallengeRepository challengeRepository;
    private final SpringDataChallengeParticipantRepository participantRepository;
    private final SpringDataChallengeRewardRepository rewardRepository;

    @Override
    public Challenge save(Challenge challenge) {
        if (challenge.getId() == null) {
            challenge.setId(UUID.randomUUID());
        }
        if (challenge.getCreatedAt() == null) {
            challenge.setCreatedAt(Instant.now());
        }
        challenge.setUpdatedAt(Instant.now());

        ChallengeJpaEntity entity = ChallengeJpaEntity.fromDomain(challenge);
        ChallengeJpaEntity saved = challengeRepository.save(entity);

        if (challenge.getRewards() != null && !challenge.getRewards().isEmpty()) {
            for (ChallengeReward reward : challenge.getRewards()) {
                if (reward.getChallengeId() == null) {
                    reward.setChallengeId(saved.getId());
                }
            }
            saveAllRewards(challenge.getRewards());
        }

        return populateRewards(saved.toDomain());
    }

    @Override
    public Optional<Challenge> findById(UUID id) {
        return challengeRepository.findById(id)
                .map(ChallengeJpaEntity::toDomain)
                .map(this::populateRewards);
    }

    @Override
    public List<Challenge> findByCommunityId(UUID communityId) {
        return challengeRepository.findByCommunityIdOrderByCreatedAtDesc(communityId)
                .stream()
                .map(ChallengeJpaEntity::toDomain)
                .map(this::populateRewards)
                .toList();
    }

    @Override
    public List<Challenge> findActiveChallengesByCommunityId(UUID communityId, Instant now) {
        return challengeRepository.findActiveChallenges(communityId, ChallengeStatus.ACTIVE, now)
                .stream()
                .map(ChallengeJpaEntity::toDomain)
                .map(this::populateRewards)
                .toList();
    }

    @Override
    public ChallengeParticipant saveParticipant(ChallengeParticipant participant) {
        ChallengeParticipantJpaEntity entity = ChallengeParticipantJpaEntity.fromDomain(participant);
        ChallengeParticipantJpaEntity saved = participantRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<ChallengeParticipant> findParticipant(UUID challengeId, UUID userId) {
        return participantRepository.findByIdChallengeIdAndIdUserId(challengeId, userId)
                .map(ChallengeParticipantJpaEntity::toDomain);
    }

    @Override
    public List<ChallengeParticipant> findActiveParticipationsByUserAndCommunity(UUID userId, UUID communityId, Instant now) {
        return participantRepository.findActiveParticipationsByUserAndCommunity(userId, communityId, now)
                .stream()
                .map(ChallengeParticipantJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<ChallengeParticipant> findParticipantsByChallengeId(UUID challengeId) {
        return participantRepository.findByIdChallengeId(challengeId)
                .stream()
                .map(ChallengeParticipantJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void saveReward(ChallengeReward reward) {
        if (reward.getId() == null) {
            reward.setId(UUID.randomUUID());
        }
        rewardRepository.save(ChallengeRewardJpaEntity.fromDomain(reward));
    }

    @Override
    public void saveAllRewards(List<ChallengeReward> rewards) {
        List<ChallengeRewardJpaEntity> entities = rewards.stream()
                .map(r -> {
                    if (r.getId() == null) {
                        r.setId(UUID.randomUUID());
                    }
                    return ChallengeRewardJpaEntity.fromDomain(r);
                })
                .toList();
        rewardRepository.saveAll(entities);
    }

    @Override
    public List<ChallengeReward> findRewardsByChallengeId(UUID challengeId) {
        return rewardRepository.findByChallengeId(challengeId)
                .stream()
                .map(ChallengeRewardJpaEntity::toDomain)
                .toList();
    }

    private Challenge populateRewards(Challenge challenge) {
        List<ChallengeReward> rewards = findRewardsByChallengeId(challenge.getId());
        challenge.setRewards(rewards);
        return challenge;
    }
}
