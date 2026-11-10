package com.studywithus.gamification.application;

import com.studywithus.gamification.domain.Badge;
import com.studywithus.gamification.domain.BadgeGrantedEvent;
import com.studywithus.gamification.domain.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AwardBadgeUseCase {

    private final BadgeRepository badgeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public boolean execute(AwardBadgeCommand command) {
        Optional<Badge> badgeOpt = badgeRepository.findById(command.badgeId());
        if (badgeOpt.isEmpty()) {
            log.warn("Badge not found with id: {}", command.badgeId());
            return false;
        }

        Badge badge = badgeOpt.get();

        if (badgeRepository.hasBadge(command.userId(), badge.getId())) {
            log.debug("User {} already possesses badge {}. Skipping assignment (idempotent).", command.userId(), badge.getId());
            return false;
        }

        badgeRepository.assignBadgeToUser(
                command.userId(),
                badge.getId(),
                Instant.now(),
                command.challengeId(),
                command.communityId()
        );

        log.info("Badge '{}' awarded to user {}.", badge.getName(), command.userId());

        eventPublisher.publishEvent(new BadgeGrantedEvent(
                command.userId(),
                badge.getId(),
                badge.getName(),
                command.communityId()
        ));

        return true;
    }
}
