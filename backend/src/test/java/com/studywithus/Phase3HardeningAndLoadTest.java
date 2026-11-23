package com.studywithus;

import com.studywithus.checkin.domain.CheckinCreatedEvent;
import com.studywithus.community.domain.Community;
import com.studywithus.community.domain.CommunityMemberRepository;
import com.studywithus.community.domain.CommunityRepository;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.media.application.MediaValidationService;
import com.studywithus.media.domain.ObjectStoragePort;
import com.studywithus.media.infrastructure.ResilientObjectStorageAdapter;
import com.studywithus.shared.domain.DomainException;
import com.studywithus.shared.infrastructure.RedisCacheCompressionHelper;
import com.studywithus.shared.infrastructure.resilience.CircuitBreaker;
import com.studywithus.social.application.FeedProjectionEventListener;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FollowRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Phase3HardeningAndLoadTest {

    @Test
    @DisplayName("Load Test: 1,000 concurrent check-ins fan-out without message loss within SLO")
    void shouldHandle1000ConcurrentCheckinFanOutsWithoutLoss() throws InterruptedException {
        UUID communityId = UUID.randomUUID();
        List<UUID> members = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            members.add(UUID.randomUUID());
        }

        CommunityMemberRepository memberRepo = mock(CommunityMemberRepository.class);
        CommunityRepository communityRepo = mock(CommunityRepository.class);
        FollowRepository followRepo = mock(FollowRepository.class);
        when(memberRepo.findActiveUserIdsByCommunityId(communityId)).thenReturn(members);
        when(communityRepo.findById(communityId)).thenReturn(Optional.of(Community.builder()
                .id(communityId)
                .name("Load Test Community")
                .visibility(CommunityVisibility.PUBLIC)
                .timezone("America/Sao_Paulo")
                .build()));
        when(followRepo.findFollowerIdsByFollowingId(any())).thenReturn(List.of());

        AtomicInteger projectedEvents = new AtomicInteger(0);
        AtomicInteger totalTimelinesUpdated = new AtomicInteger(0);
        FeedTimelinePort countingTimelinePort = new FeedTimelinePort() {
            @Override
            public void addCheckinToTimelines(UUID checkinId, Instant createdAt, UUID cId, Collection<UUID> userIds) {
                projectedEvents.incrementAndGet();
                totalTimelinesUpdated.addAndGet(userIds.size());
            }
            @Override public void removeCheckinFromTimelines(UUID checkinId, UUID cId, Collection<UUID> userIds) {}
            @Override public com.studywithus.social.domain.FeedTimelineSlice getUserTimeline(UUID userId, Instant cursorTime, UUID cursorId, int limit) { return com.studywithus.social.domain.FeedTimelineSlice.miss(); }
            @Override public com.studywithus.social.domain.FeedTimelineSlice getCommunityTimeline(UUID cId, Instant cursorTime, UUID cursorId, int limit) { return com.studywithus.social.domain.FeedTimelineSlice.miss(); }
            @Override public void warmUserTimeline(UUID userId, List<com.studywithus.social.domain.FeedTimelineEntry> entries) {}
            @Override public void warmCommunityTimeline(UUID cId, List<com.studywithus.social.domain.FeedTimelineEntry> entries) {}
            @Override public void invalidateUserTimeline(UUID userId) {}
            @Override public void invalidateCommunityTimeline(UUID cId) {}
        };

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        FeedProjectionEventListener listener = new FeedProjectionEventListener(
                countingTimelinePort, memberRepo, communityRepo, followRepo, null, meterRegistry
        );

        int totalCheckins = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(totalCheckins);
        long startNanos = System.nanoTime();

        for (int i = 0; i < totalCheckins; i++) {
            UUID authorId = members.get(i % members.size());
            pool.submit(() -> {
                try {
                    listener.onCheckinCreated(new CheckinCreatedEvent(UUID.randomUUID(), authorId, communityId, Instant.now()));
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        pool.shutdown();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

        assertThat(finished).isTrue();
        assertThat(projectedEvents.get()).isEqualTo(totalCheckins);
        assertThat(totalTimelinesUpdated.get()).isEqualTo(totalCheckins * 50);
        assertThat(meterRegistry.counter("studywithus_feed_fanout_total").count()).isEqualTo(1000.0);
        assertThat(elapsedMs).isLessThan(10_000L);
    }

    @Test
    @DisplayName("Resilience Test: Circuit Breaker isolates Object Storage outage and recovers")
    void shouldIsolateObjectStorageFailureWithCircuitBreakerAndRecover() throws InterruptedException {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        CircuitBreaker cb = new CircuitBreaker("object-storage-test", 3, Duration.ofMillis(120), meterRegistry);

        ObjectStoragePort failingStorage = mock(ObjectStoragePort.class);
        when(failingStorage.generatePresignedUploadUrl(any(), any(), any(Long.class), any()))
                .thenThrow(new RuntimeException("S3 timeout"));

        ResilientObjectStorageAdapter resilient = new ResilientObjectStorageAdapter(failingStorage, cb);

        // 3 consecutive failures trip the circuit
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> resilient.generatePresignedUploadUrl("k", "image/png", 100L, Duration.ofMinutes(5)))
                    .isInstanceOf(RuntimeException.class);
        }
        assertThat(resilient.getCircuitState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Next call is immediately short-circuited with 503 STORAGE_CIRCUIT_OPEN
        assertThatThrownBy(() -> resilient.generatePresignedUploadUrl("k", "image/png", 100L, Duration.ofMinutes(5)))
                .isInstanceOf(DomainException.class)
                .satisfies(ex -> {
                    DomainException de = (DomainException) ex;
                    assertThat(de.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(de.getCode()).isEqualTo("STORAGE_CIRCUIT_OPEN");
                });

        // Wait for openDuration -> HALF_OPEN -> storage recovers -> CLOSED
        Thread.sleep(150);
        org.mockito.Mockito.doReturn(
                new ObjectStoragePort.PresignedUploadResult("https://cdn.example/ok", Instant.now(), java.util.Map.of())
        ).when(failingStorage).generatePresignedUploadUrl(any(), any(), any(Long.class), any());

        ObjectStoragePort.PresignedUploadResult ok = resilient.generatePresignedUploadUrl("k", "image/png", 100L, Duration.ofMinutes(5));
        assertThat(ok.presignedUrl()).isEqualTo("https://cdn.example/ok");
        assertThat(resilient.getCircuitState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Media Security Audit: blocks path traversal, SVG/script polyglots, and SSRF targets")
    void shouldBlockMaliciousUploadsPathTraversalAndSsrf() {
        assertThatThrownBy(() -> MediaValidationService.extractAndValidateExtension("../../etc/passwd.png"))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("INVALID_FILENAME");

        assertThatThrownBy(() -> MediaValidationService.validateSafeStorageKey("uploads/../../secret"))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("INVALID_STORAGE_KEY");

        byte[] svgPayload = "<svg onload=alert(1)><script>evil()</script></svg>".getBytes(StandardCharsets.UTF_8);
        assertThat(MediaValidationService.detectRealMimeType(svgPayload)).isEmpty();

        assertThatThrownBy(() -> MediaValidationService.validateSafeExternalUrl("http://127.0.0.1:8080/actuator"))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("SSRF_BLOCKED");

        assertThatThrownBy(() -> MediaValidationService.validateSafeExternalUrl("http://169.254.169.254/latest/meta-data/"))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("SSRF_BLOCKED");

        assertThatThrownBy(() -> MediaValidationService.validateSafeExternalUrl("file:///etc/passwd"))
                .isInstanceOf(DomainException.class)
                .extracting("code").isEqualTo("UNSAFE_URL_SCHEME");
    }

    @Test
    @DisplayName("Cache Compression: compresses large Redis payloads and round-trips losslessly")
    void shouldCompressAndDecompressLargeCachePayloads() {
        String largeJson = "{\"items\":[" + "\"studywithus-cached-entry\",".repeat(60) + "\"end\"]}";
        String compressed = RedisCacheCompressionHelper.compressIfBeneficial(largeJson);

        assertThat(compressed).startsWith("gz:");
        assertThat(compressed.length()).isLessThan(largeJson.length());
        assertThat(RedisCacheCompressionHelper.decompressIfNeeded(compressed)).isEqualTo(largeJson);
    }
}
