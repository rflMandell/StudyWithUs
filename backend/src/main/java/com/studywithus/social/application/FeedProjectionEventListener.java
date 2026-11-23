package com.studywithus.social.application;

import com.studywithus.checkin.domain.Checkin;
import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.checkin.domain.CheckinDeletedEvent;
import com.studywithus.checkin.domain.CheckinRepository;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberJoinedEvent;
import com.studywithus.community.domain.CommunityMemberRemovedEvent;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.moderation.domain.ModerationAction;
import com.studywithus.moderation.domain.ReportResolvedEvent;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FollowRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class FeedProjectionEventListener {

    private final FeedTimelinePort feedTimelinePort;
    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityRepository communityRepository;
    private final FollowRepository followRepository;
    private final CheckinRepository checkinRepository;
    private final Counter fanoutCounter;
    private final Counter fanoutRecipientsCounter;
    private final Timer fanoutTimer;

    public FeedProjectionEventListener(
            FeedTimelinePort feedTimelinePort,
            CommunityMemberRepository communityMemberRepository,
            CommunityRepository communityRepository,
            FollowRepository followRepository,
            CheckinRepository checkinRepository,
            @Autowired(required = false) MeterRegistry meterRegistry
    ) {
        this.feedTimelinePort = feedTimelinePort;
        this.communityMemberRepository = communityMemberRepository;
        this.communityRepository = communityRepository;
        this.followRepository = followRepository;
        this.checkinRepository = checkinRepository;
        this.fanoutCounter = meterRegistry != null
                ? Counter.builder("studywithus_feed_fanout_total")
                        .description("Total number of feed fan-out executions")
                        .register(meterRegistry)
                : null;
        this.fanoutRecipientsCounter = meterRegistry != null
                ? Counter.builder("studywithus_feed_fanout_recipients_total")
                        .description("Total number of user timelines updated by feed fan-out")
                        .register(meterRegistry)
                : null;
        this.fanoutTimer = meterRegistry != null
                ? Timer.builder("studywithus_feed_fanout_duration_seconds")
                        .description("Time spent executing async feed fan-out")
                        .register(meterRegistry)
                : null;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCheckinCreated(CheckinCreatedEvent event) {
        Timer.Sample sample = fanoutTimer != null ? Timer.start() : null;
        try {
            Set<UUID> recipients = resolveRecipients(event.communityId(), event.userId());
            feedTimelinePort.addCheckinToTimelines(
                    event.checkinId(),
                    event.occurredAt(),
                    event.communityId(),
                    recipients
            );
            if (fanoutCounter != null) {
                fanoutCounter.increment();
            }
            if (fanoutRecipientsCounter != null) {
                fanoutRecipientsCounter.increment(recipients.size());
            }
            log.debug("Projected checkinId={} to community={} and {} user timelines",
                    event.checkinId(), event.communityId(), recipients.size());
        } catch (Exception e) {
            log.warn("Failed async feed projection for checkinId={}: {}", event.checkinId(), e.getMessage());
        } finally {
            if (sample != null && fanoutTimer != null) {
                sample.stop(fanoutTimer);
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCheckinDeleted(CheckinDeletedEvent event) {
        removeCheckinFromTimelines(event.checkinId(), event.communityId(), event.userId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onReportResolved(ReportResolvedEvent event) {
        if (event.action() != ModerationAction.REMOVE_CONTENT || event.targetType() != ReportTargetType.CHECKIN) {
            return;
        }
        checkinRepository.findByIdIncludingDeleted(event.targetId()).ifPresent(checkin ->
                removeCheckinFromTimelines(checkin.getId(), checkin.getCommunityId(), checkin.getUserId())
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCommunityMemberJoined(CommunityMemberJoinedEvent event) {
        feedTimelinePort.invalidateUserTimeline(event.userId());
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCommunityMemberRemoved(CommunityMemberRemovedEvent event) {
        feedTimelinePort.invalidateUserTimeline(event.targetUserId());
    }

    private void removeCheckinFromTimelines(UUID checkinId, UUID communityId, UUID authorId) {
        try {
            Set<UUID> recipients = resolveRecipients(communityId, authorId);
            feedTimelinePort.removeCheckinFromTimelines(checkinId, communityId, recipients);
            log.debug("Removed checkinId={} from community={} and {} user timelines",
                    checkinId, communityId, recipients.size());
        } catch (Exception e) {
            log.warn("Failed async feed removal for checkinId={}: {}", checkinId, e.getMessage());
        }
    }

    private Set<UUID> resolveRecipients(UUID communityId, UUID authorId) {
        Set<UUID> recipients = new LinkedHashSet<>(communityMemberRepository.findActiveUserIdsByCommunityId(communityId));
        recipients.add(authorId);

        boolean isPublicCommunity = communityRepository.findById(communityId)
                .filter(Community::isActive)
                .map(Community::isPublic)
                .orElse(false);

        if (isPublicCommunity) {
            recipients.addAll(followRepository.findFollowerIdsByFollowingId(authorId));
        }

        return recipients;
    }
}
