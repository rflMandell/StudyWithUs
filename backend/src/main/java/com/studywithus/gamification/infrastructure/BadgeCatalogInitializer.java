package com.studywithus.gamification.infrastructure;

import com.studywithus.gamification.domain.Badge;
import com.studywithus.gamification.domain.BadgeConstants;
import com.studywithus.gamification.domain.BadgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BadgeCatalogInitializer {

    private final BadgeRepository badgeRepository;

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void initializeCatalog() {
        seedBadge(
                BadgeConstants.BADGE_FIRST_CHECKIN_ID,
                BadgeConstants.BADGE_FIRST_CHECKIN_NAME,
                BadgeConstants.BADGE_FIRST_CHECKIN_DESC,
                BadgeConstants.BADGE_FIRST_CHECKIN_IMG
        );
        seedBadge(
                BadgeConstants.BADGE_STREAK_7_ID,
                BadgeConstants.BADGE_STREAK_7_NAME,
                BadgeConstants.BADGE_STREAK_7_DESC,
                BadgeConstants.BADGE_STREAK_7_IMG
        );
        seedBadge(
                BadgeConstants.BADGE_STREAK_30_ID,
                BadgeConstants.BADGE_STREAK_30_NAME,
                BadgeConstants.BADGE_STREAK_30_DESC,
                BadgeConstants.BADGE_STREAK_30_IMG
        );
        seedBadge(
                BadgeConstants.BADGE_COMMUNITY_CREATOR_ID,
                BadgeConstants.BADGE_COMMUNITY_CREATOR_NAME,
                BadgeConstants.BADGE_COMMUNITY_CREATOR_DESC,
                BadgeConstants.BADGE_COMMUNITY_CREATOR_IMG
        );
        seedBadge(
                BadgeConstants.BADGE_POINTS_100_ID,
                BadgeConstants.BADGE_POINTS_100_NAME,
                BadgeConstants.BADGE_POINTS_100_DESC,
                BadgeConstants.BADGE_POINTS_100_IMG
        );
        seedBadge(
                BadgeConstants.BADGE_LEAGUE_CHAMPION_ID,
                BadgeConstants.BADGE_LEAGUE_CHAMPION_NAME,
                BadgeConstants.BADGE_LEAGUE_CHAMPION_DESC,
                BadgeConstants.BADGE_LEAGUE_CHAMPION_IMG
        );
        seedBadge(
                BadgeConstants.BADGE_LEAGUE_GOLD_ID,
                BadgeConstants.BADGE_LEAGUE_GOLD_NAME,
                BadgeConstants.BADGE_LEAGUE_GOLD_DESC,
                BadgeConstants.BADGE_LEAGUE_GOLD_IMG
        );
        seedBadge(
                BadgeConstants.BADGE_LEAGUE_DIAMOND_ID,
                BadgeConstants.BADGE_LEAGUE_DIAMOND_NAME,
                BadgeConstants.BADGE_LEAGUE_DIAMOND_DESC,
                BadgeConstants.BADGE_LEAGUE_DIAMOND_IMG
        );
        seedBadge(
                BadgeConstants.BADGE_LEAGUE_MASTER_ID,
                BadgeConstants.BADGE_LEAGUE_MASTER_NAME,
                BadgeConstants.BADGE_LEAGUE_MASTER_DESC,
                BadgeConstants.BADGE_LEAGUE_MASTER_IMG
        );
    }

    private void seedBadge(UUID id, String name, String description, String imageUrl) {
        if (badgeRepository.findById(id).isEmpty() && badgeRepository.findByName(name).isEmpty()) {
            badgeRepository.saveBadge(Badge.builder()
                    .id(id)
                    .name(name)
                    .description(description)
                    .imageUrl(imageUrl)
                    .createdAt(Instant.now())
                    .build());
            log.info("Initialized badge in catalog: {}", name);
        }
    }
}
