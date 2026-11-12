package com.studywithus.shared.infrastructure.observability;

import com.studywithus.challenge.domain.ChallengeParticipantJoinedEvent;
import com.studywithus.gamification.domain.BadgeGrantedEvent;
import com.studywithus.moderation.domain.ReportResolvedEvent;
import com.studywithus.social.domain.CommentCreatedEvent;
import com.studywithus.subscription.domain.SubscriptionActivatedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Phase2MetricsListener {

    private final MeterRegistry meterRegistry;
    private final Counter subscriptionConversionsCounter;
    private final Counter commentsCreatedCounter;
    private final Counter challengesJoinedCounter;
    private final Counter badgesAwardedCounter;

    public Phase2MetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.subscriptionConversionsCounter = Counter.builder("studywithus_subscription_conversions_total")
                .description("Total number of successful subscription conversions to Premium")
                .register(meterRegistry);
        this.commentsCreatedCounter = Counter.builder("studywithus_comments_created_total")
                .description("Total number of social comments created on check-ins")
                .register(meterRegistry);
        this.challengesJoinedCounter = Counter.builder("studywithus_challenge_participations_total")
                .description("Total number of participants joined in challenges")
                .register(meterRegistry);
        this.badgesAwardedCounter = Counter.builder("studywithus_badges_awarded_total")
                .description("Total number of gamification badges granted")
                .register(meterRegistry);
    }

    @EventListener
    public void onSubscriptionActivated(SubscriptionActivatedEvent event) {
        subscriptionConversionsCounter.increment();
        log.info("Metric incremented: studywithus_subscription_conversions_total (userId={})", event.userId());
    }

    @EventListener
    public void onReportResolved(ReportResolvedEvent event) {
        Counter.builder("studywithus_reports_resolved_total")
                .description("Total number of moderation reports resolved")
                .tag("action", event.action().name())
                .tag("targetType", event.targetType().name())
                .register(meterRegistry)
                .increment();
        log.info("Metric incremented: studywithus_reports_resolved_total (reportId={}, action={})", event.reportId(), event.action());
    }

    @EventListener
    public void onCommentCreated(CommentCreatedEvent event) {
        commentsCreatedCounter.increment();
        log.debug("Metric incremented: studywithus_comments_created_total (commentId={})", event.commentId());
    }

    @EventListener
    public void onChallengeParticipantJoined(ChallengeParticipantJoinedEvent event) {
        challengesJoinedCounter.increment();
        log.info("Metric incremented: studywithus_challenge_participations_total (challengeId={}, userId={})", event.challengeId(), event.userId());
    }

    @EventListener
    public void onBadgeGranted(BadgeGrantedEvent event) {
        badgesAwardedCounter.increment();
        log.info("Metric incremented: studywithus_badges_awarded_total (badgeName={}, userId={})", event.badgeName(), event.userId());
    }
}
