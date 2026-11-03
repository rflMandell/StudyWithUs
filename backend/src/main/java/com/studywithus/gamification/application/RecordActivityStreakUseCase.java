package com.studywithus.gamification.application;

import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.gamification.domain.CommunityFreeDayRepository;
import com.studywithus.gamification.domain.CommunityStreak;
import com.studywithus.gamification.domain.CommunityStreakRepository;
import com.studywithus.gamification.domain.StreakUpdatedEvent;
import com.studywithus.gamification.domain.UserStreak;
import com.studywithus.gamification.domain.UserStreakRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordActivityStreakUseCase {

    private final UserStreakRepository userStreakRepository;
    private final CommunityStreakRepository communityStreakRepository;
    private final CommunityFreeDayRepository communityFreeDayRepository;
    private final CommunityRepository communityRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public StreakResult execute(UUID userId, UUID communityId, Instant occurredAt) {
        ZoneId zoneId = resolveZoneId(communityId);
        LocalDate activityDate = (occurredAt != null ? occurredAt : Instant.now()).atZone(zoneId).toLocalDate();
        return recordOnDate(userId, communityId, activityDate);
    }

    @Transactional
    public StreakResult recordOnDate(UUID userId, UUID communityId, LocalDate activityDate) {
        List<Integer> freeDays = communityId != null
                ? communityFreeDayRepository.findDaysOfWeekByCommunityId(communityId)
                : List.of();

        UserStreak userStreak = userStreakRepository.findByUserId(userId)
                .orElseGet(() -> UserStreak.empty(userId));
        userStreak.recordActivity(activityDate, freeDays);
        userStreak = userStreakRepository.save(userStreak);

        CommunityStreak communityStreak = null;
        if (communityId != null) {
            communityStreak = communityStreakRepository.findByCommunityIdAndUserId(communityId, userId)
                    .orElseGet(() -> CommunityStreak.empty(communityId, userId));
            communityStreak.recordActivity(activityDate, freeDays);
            communityStreak = communityStreakRepository.save(communityStreak);
        }

        int commStreakVal = communityStreak != null ? communityStreak.getCurrentStreak() : 0;

        eventPublisher.publishEvent(new StreakUpdatedEvent(
                userId,
                communityId,
                userStreak.getCurrentStreak(),
                commStreakVal,
                activityDate
        ));

        return new StreakResult(
                userStreak.getCurrentStreak(),
                userStreak.getLongestStreak(),
                commStreakVal,
                communityStreak != null ? communityStreak.getLongestStreak() : 0,
                activityDate
        );
    }

    private ZoneId resolveZoneId(UUID communityId) {
        if (communityId == null) {
            return ZoneId.of("America/Sao_Paulo");
        }
        return communityRepository.findById(communityId)
                .map(Community::getTimezone)
                .map(tz -> {
                    try {
                        return ZoneId.of(tz);
                    } catch (Exception e) {
                        return ZoneId.of("America/Sao_Paulo");
                    }
                })
                .orElse(ZoneId.of("America/Sao_Paulo"));
    }

    public record StreakResult(
            int userCurrentStreak,
            int userLongestStreak,
            int communityCurrentStreak,
            int communityLongestStreak,
            LocalDate lastActivityDate
    ) {}
}
