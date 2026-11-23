package com.studywithus.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.social.domain.FeedTimelineEntry;
import com.studywithus.social.domain.FeedTimelinePort;
import com.studywithus.social.domain.FeedTimelineSlice;
import com.studywithus.social.presentation.FeedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterializedFeedIntegrationTest {

    @TestConfiguration
    static class InMemoryZSetFeedConfig {
        @Bean
        @Primary
        InMemoryZSetFeedTimelineAdapter inMemoryZSetFeedTimelineAdapter() {
            return new InMemoryZSetFeedTimelineAdapter();
        }
    }

    /**
     * In-memory ZSET simulation of RedisFeedTimelineAdapter to verify full async fan-out,
     * cursor pagination, atomic deletion, and Redis-down fallback to PostgreSQL end-to-end.
     */
    static class InMemoryZSetFeedTimelineAdapter implements FeedTimelinePort {
        private final Map<String, NavigableSet<ZEntry>> zsets = new ConcurrentHashMap<>();
        private final AtomicBoolean simulateRedisOutage = new AtomicBoolean(false);

        record ZEntry(long scoreMillis, UUID id) implements Comparable<ZEntry> {
            @Override
            public int compareTo(ZEntry o) {
                int cmp = Long.compare(o.scoreMillis, this.scoreMillis); // DESC
                return cmp != 0 ? cmp : o.id.toString().compareTo(this.id.toString()); // DESC
            }
        }

        void setSimulateRedisOutage(boolean outage) {
            simulateRedisOutage.set(outage);
        }

        void clear() {
            zsets.clear();
        }

        boolean hasUserTimeline(UUID userId) {
            return zsets.containsKey("feed:user:" + userId);
        }

        boolean hasCommunityTimeline(UUID communityId) {
            return zsets.containsKey("feed:community:" + communityId);
        }

        @Override
        public void addCheckinToTimelines(UUID checkinId, Instant createdAt, UUID communityId, Collection<UUID> userIds) {
            if (simulateRedisOutage.get()) return;
            ZEntry entry = new ZEntry(createdAt.toEpochMilli(), checkinId);
            if (communityId != null) {
                zsets.computeIfAbsent("feed:community:" + communityId, k -> new TreeSet<>()).add(entry);
            }
            if (userIds != null) {
                for (UUID uid : userIds) {
                    zsets.computeIfAbsent("feed:user:" + uid, k -> new TreeSet<>()).add(entry);
                }
            }
        }

        @Override
        public void removeCheckinFromTimelines(UUID checkinId, UUID communityId, Collection<UUID> userIds) {
            if (simulateRedisOutage.get()) return;
            if (communityId != null) {
                NavigableSet<ZEntry> set = zsets.get("feed:community:" + communityId);
                if (set != null) set.removeIf(e -> e.id().equals(checkinId));
            }
            if (userIds != null) {
                for (UUID uid : userIds) {
                    NavigableSet<ZEntry> set = zsets.get("feed:user:" + uid);
                    if (set != null) set.removeIf(e -> e.id().equals(checkinId));
                }
            }
        }

        @Override
        public FeedTimelineSlice getUserTimeline(UUID userId, Instant cursorTime, UUID cursorId, int limit) {
            return getSlice("feed:user:" + userId, cursorTime, cursorId, limit);
        }

        @Override
        public FeedTimelineSlice getCommunityTimeline(UUID communityId, Instant cursorTime, UUID cursorId, int limit) {
            return getSlice("feed:community:" + communityId, cursorTime, cursorId, limit);
        }

        private FeedTimelineSlice getSlice(String key, Instant cursorTime, UUID cursorId, int limit) {
            if (simulateRedisOutage.get()) {
                return FeedTimelineSlice.miss();
            }
            NavigableSet<ZEntry> set = zsets.get(key);
            if (set == null) {
                return FeedTimelineSlice.miss();
            }
            List<UUID> result = new ArrayList<>();
            synchronized (set) {
                for (ZEntry e : set) {
                    if (cursorTime != null) {
                        long cursorMillis = cursorTime.toEpochMilli();
                        boolean older = e.scoreMillis() < cursorMillis
                                || (e.scoreMillis() == cursorMillis && cursorId != null
                                && e.id().toString().compareTo(cursorId.toString()) < 0);
                        if (!older) continue;
                    }
                    result.add(e.id());
                    if (result.size() >= limit) break;
                }
            }
            return FeedTimelineSlice.hit(result);
        }

        @Override
        public void warmUserTimeline(UUID userId, List<FeedTimelineEntry> entries) {
            if (simulateRedisOutage.get()) return;
            NavigableSet<ZEntry> set = zsets.computeIfAbsent("feed:user:" + userId, k -> new TreeSet<>());
            for (FeedTimelineEntry e : entries) {
                set.add(new ZEntry(e.createdAt().toEpochMilli(), e.checkinId()));
            }
        }

        @Override
        public void warmCommunityTimeline(UUID communityId, List<FeedTimelineEntry> entries) {
            if (simulateRedisOutage.get()) return;
            NavigableSet<ZEntry> set = zsets.computeIfAbsent("feed:community:" + communityId, k -> new TreeSet<>());
            for (FeedTimelineEntry e : entries) {
                set.add(new ZEntry(e.createdAt().toEpochMilli(), e.checkinId()));
            }
        }

        @Override
        public void invalidateUserTimeline(UUID userId) {
            zsets.remove("feed:user:" + userId);
        }

        @Override
        public void invalidateCommunityTimeline(UUID communityId) {
            zsets.remove("feed:community:" + communityId);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InMemoryZSetFeedTimelineAdapter timelineAdapter;

    @BeforeEach
    void resetAdapter() {
        timelineAdapter.setSimulateRedisOutage(false);
        timelineAdapter.clear();
    }

    @Test
    @DisplayName("Sprint 21 End-to-End: Async fan-out, cursor pagination, community feed, atomic delete, and PostgreSQL fallback")
    void shouldProjectFeedAsynchronouslyPaginateWithCursorDeleteAtomicallyAndFallbackOnOutage() throws Exception {
        // 1. Register Author & Member
        AuthResponse author = registerUser("mat_author_" + UUID.randomUUID().toString().substring(0, 6));
        AuthResponse member = registerUser("mat_member_" + UUID.randomUUID().toString().substring(0, 6));

        // 2. Author creates public community & Member joins
        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + author.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                "Comunidade High Performance",
                                "Testando feed materializado",
                                null,
                                CommunityVisibility.PUBLIC,
                                "America/Sao_Paulo"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID communityId = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class).id();

        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isCreated());

        // 3. Author creates 3 check-ins -> async fan-out populates Redis timelines
        UUID c1 = createCheckin(author.accessToken(), communityId, "Sessão 1");
        Thread.sleep(5);
        UUID c2 = createCheckin(author.accessToken(), communityId, "Sessão 2");
        Thread.sleep(5);
        UUID c3 = createCheckin(author.accessToken(), communityId, "Sessão 3");

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(timelineAdapter.hasUserTimeline(member.userId())).isTrue();
            assertThat(timelineAdapter.hasCommunityTimeline(communityId)).isTrue();
            FeedTimelineSlice slice = timelineAdapter.getUserTimeline(member.userId(), null, null, 10);
            assertThat(slice.checkinIds()).containsExactly(c3, c2, c1);
        });

        // 4. Member fetches user feed with cursor pagination (limit=2 -> page 1 + page 2)
        MvcResult page1Res = mockMvc.perform(get("/api/v1/feed?limit=2")
                        .header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(true))
                .andReturn();
        FeedResponse page1 = objectMapper.readValue(page1Res.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(page1.content()).extracting(r -> r.id()).containsExactly(c3, c2);

        MvcResult page2Res = mockMvc.perform(get("/api/v1/feed?limit=2&cursor=" + page1.nextCursor())
                        .header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasMore").value(false))
                .andReturn();
        FeedResponse page2 = objectMapper.readValue(page2Res.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(page2.content()).extracting(r -> r.id()).containsExactly(c1);

        // 5. Community feed endpoint also serves from materialized timeline
        mockMvc.perform(get("/api/v1/communities/" + communityId + "/feed?limit=2")
                        .header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(c3.toString()))
                .andExpect(jsonPath("$.content[1].id").value(c2.toString()));

        // 6. Author deletes c3 -> atomic removal from Redis timelines
        mockMvc.perform(delete("/api/v1/checkins/" + c3)
                        .header("Authorization", "Bearer " + author.accessToken()))
                .andExpect(status().isNoContent());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            FeedTimelineSlice slice = timelineAdapter.getUserTimeline(member.userId(), null, null, 10);
            assertThat(slice.checkinIds()).containsExactly(c2, c1);
        });

        // 7. Simulate Redis outage -> feed seamlessly falls back to PostgreSQL
        timelineAdapter.setSimulateRedisOutage(true);
        mockMvc.perform(get("/api/v1/feed?limit=10")
                        .header("Authorization", "Bearer " + member.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(c2.toString()))
                .andExpect(jsonPath("$.content[1].id").value(c1.toString()));
    }

    private AuthResponse registerUser(String username) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                username,
                                "User " + username,
                                username + "@uni.edu",
                                "pass123456",
                                LocalDate.of(2001, 1, 1),
                                "USP", "Computação", "Meta", "Bio", "dev-" + username
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
    }

    private UUID createCheckin(String token, UUID communityId, String caption) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                caption,
                                List.of()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), CheckinResponse.class).id();
    }
}
