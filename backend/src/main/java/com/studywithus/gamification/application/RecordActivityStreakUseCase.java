package com.studywithus.gamification.application;

import com.studywithus.checkin.domain.CheckinRepository;
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
import java.time.ZoneOffset;
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
    private final CheckinRepository checkinRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public StreakResult execute(UUID userId, UUID communityId, Instant occurredAt) {
        Instant effectiveTime = occurredAt != null ? occurredAt : Instant.now();
        LocalDate userActivityDate = effectiveTime.atZone(ZoneOffset.UTC).toLocalDate();
        ZoneId communityZone = resolveZoneId(communityId);
        LocalDate communityActivityDate = effectiveTime.atZone(communityZone).toLocalDate();
        return recordOnDates(userId, communityId, userActivityDate, communityActivityDate);
    }

    @Transactional
    public StreakResult recordOnDate(UUID userId, UUID communityId, LocalDate activityDate) {
        return recordOnDates(userId, communityId, activityDate, activityDate);
    }

    private StreakResult recordOnDates(
            UUID userId,
            UUID communityId,
            LocalDate userActivityDate,
            LocalDate communityActivityDate
    ) {
        List<Integer> freeDays = communityId != null
                ? communityFreeDayRepository.findDaysOfWeekByCommunityId(communityId)
                : List.of();

        UserStreak userStreak = userStreakRepository.findByUserId(userId)
                .orElseGet(() -> UserStreak.empty(userId));
        userStreak.recordActivity(userActivityDate, List.of());
        userStreak = userStreakRepository.save(userStreak);

        CommunityStreak communityStreak = null;
        if (communityId != null) {
            communityStreak = communityStreakRepository.findByCommunityIdAndUserId(communityId, userId)
                    .orElseGet(() -> CommunityStreak.empty(communityId, userId));
            communityStreak.recordActivity(communityActivityDate, freeDays);
            communityStreak = communityStreakRepository.save(communityStreak);
        }

        int commStreakVal = communityStreak != null ? communityStreak.getCurrentStreak() : 0;

        eventPublisher.publishEvent(new StreakUpdatedEvent(
                userId,
                communityId,
                userStreak.getCurrentStreak(),
                commStreakVal,
                communityActivityDate
        ));

        return new StreakResult(
                userStreak.getCurrentStreak(),
                userStreak.getLongestStreak(),
                commStreakVal,
                communityStreak != null ? communityStreak.getLongestStreak() : 0,
                communityActivityDate
        );
    }

    @Transactional
    public void recomputeOnCheckinDeleted(UUID userId, UUID communityId, Instant occurredAt) {
        Instant effectiveTime = occurredAt != null ? occurredAt : Instant.now();

        LocalDate utcDate = effectiveTime.atZone(ZoneOffset.UTC).toLocalDate();
        Instant utcStart = utcDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant utcEnd = utcDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        if (!checkinRepository.existsActiveByUserIdBetween(userId, utcStart, utcEnd)) {
            UserStreak recomputed = UserStreak.empty(userId);
            for (Instant createdAt : checkinRepository.findActiveCreatedAtByUserId(userId)) {
                recomputed.recordActivity(createdAt.atZone(ZoneOffset.UTC).toLocalDate(), List.of());
            }
            userStreakRepository.save(recomputed);
        }

        if (communityId != null) {
            ZoneId communityZone = resolveZoneId(communityId);
            LocalDate commDate = effectiveTime.atZone(communityZone).toLocalDate();
            Instant commStart = commDate.atStartOfDay(communityZone).toInstant();
            Instant commEnd = commDate.plusDays(1).atStartOfDay(communityZone).toInstant();

            if (!checkinRepository.existsActiveByUserIdAndCommunityIdBetween(userId, communityId, commStart, commEnd)) {
                List<Integer> freeDays = communityFreeDayRepository.findDaysOfWeekByCommunityId(communityId);
                CommunityStreak recomputedComm = CommunityStreak.empty(communityId, userId);
                for (Instant createdAt : checkinRepository.findActiveCreatedAtByUserIdAndCommunityId(userId, communityId)) {
                    recomputedComm.recordActivity(createdAt.atZone(communityZone).toLocalDate(), freeDays);
                }
                communityStreakRepository.save(recomputedComm);
            }
        }
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
