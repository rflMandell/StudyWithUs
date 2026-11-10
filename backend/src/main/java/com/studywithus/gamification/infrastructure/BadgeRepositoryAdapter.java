package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.Badge;
import com.studywithus.gamification.domain.BadgeRepository;
import com.studywithus.gamification.domain.UserBadge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BadgeRepositoryAdapter implements BadgeRepository {

    private final SpringDataBadgeRepository badgeRepository;
    private final SpringDataUserBadgeRepository userBadgeRepository;

    @Override
    public Badge saveBadge(Badge badge) {
        BadgeJpaEntity entity = BadgeJpaEntity.fromDomain(badge);
        return badgeRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Badge> findById(UUID id) {
        return badgeRepository.findById(id).map(BadgeJpaEntity::toDomain);
    }

    @Override
    public Optional<Badge> findByName(String name) {
        return badgeRepository.findByName(name).map(BadgeJpaEntity::toDomain);
    }

    @Override
    public List<Badge> findAllBadges() {
        return badgeRepository.findAll().stream()
                .map(BadgeJpaEntity::toDomain)
                .toList();
    }

    @Override
    public List<UserBadge> findUserBadges(UUID userId) {
        return userBadgeRepository.findByUserIdWithBadge(userId).stream()
                .map(ub -> UserBadge.builder()
                        .userId(ub.getId().getUserId())
                        .badgeId(ub.getId().getBadgeId())
                        .name(ub.getBadge() != null ? ub.getBadge().getName() : "Conquista")
                        .description(ub.getBadge() != null ? ub.getBadge().getDescription() : "")
                        .imageUrl(ub.getBadge() != null ? ub.getBadge().getImageUrl() : null)
                        .awardedAt(ub.getAwardedAt())
                        .challengeId(ub.getChallengeId())
                        .communityId(ub.getCommunityId())
                        .build())
                .toList();
    }

    @Override
    public boolean hasBadge(UUID userId, UUID badgeId) {
        return userBadgeRepository.existsById(new UserBadgeId(userId, badgeId));
    }

    @Override
    public void assignBadgeToUser(UUID userId, UUID badgeId, Instant awardedAt, UUID challengeId, UUID communityId) {
        BadgeJpaEntity badgeRef = badgeRepository.findById(badgeId).orElse(null);
        UserBadgeJpaEntity entity = UserBadgeJpaEntity.builder()
                .id(new UserBadgeId(userId, badgeId))
                .badge(badgeRef)
                .awardedAt(awardedAt != null ? awardedAt : Instant.now())
                .challengeId(challengeId)
                .communityId(communityId)
                .build();
        userBadgeRepository.save(entity);
    }
}
