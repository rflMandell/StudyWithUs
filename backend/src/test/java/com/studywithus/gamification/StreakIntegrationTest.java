package com.studywithus.gamification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.gamification.application.RecordActivityStreakUseCase;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StreakIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecordActivityStreakUseCase recordActivityStreakUseCase;

    private static String token;
    private static UUID userId;
    private static UUID communityId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register user and create community")
    void setup() throws Exception {
        MvcResult resUser = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "streak_user",
                                "Streak Runner",
                                "streak@uni.edu",
                                "pass123456",
                                LocalDate.of(2000, 1, 1),
                                "USP", "Física", "Meta", "Bio", "dev-streak"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(resUser.getResponse().getContentAsString(), AuthResponse.class);
        token = auth.accessToken();
        userId = auth.userId();

        MvcResult resComm = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                "Comunidade Streak Teste",
                                "Descrição",
                                null,
                                CommunityVisibility.PUBLIC,
                                "America/Sao_Paulo"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        communityId = objectMapper.readValue(resComm.getResponse().getContentAsString(), CommunityResponse.class).id();
    }

    @Test
    @Order(2)
    @DisplayName("Demonstrable test: Day 1 (1) -> Day 2 (2) -> Day 3 (3) -> Day 5 without Day 4 (break to 1)")
    void shouldExecuteRoadmapStreakSequence() throws Exception {
        LocalDate day1 = LocalDate.of(2026, 11, 1);
        LocalDate day2 = LocalDate.of(2026, 11, 2);
        LocalDate day3 = LocalDate.of(2026, 11, 3);
        LocalDate day5 = LocalDate.of(2026, 11, 5);

        // Day 1: Activity performed -> Streak is 1
        var resDay1 = recordActivityStreakUseCase.recordOnDate(userId, communityId, day1);
        assertThat(resDay1.userCurrentStreak()).isEqualTo(1);
        assertThat(resDay1.communityCurrentStreak()).isEqualTo(1);

        // Day 2: Activity performed -> Streak is 2
        var resDay2 = recordActivityStreakUseCase.recordOnDate(userId, communityId, day2);
        assertThat(resDay2.userCurrentStreak()).isEqualTo(2);
        assertThat(resDay2.communityCurrentStreak()).isEqualTo(2);

        // Day 3: Activity performed -> Streak is 3
        var resDay3 = recordActivityStreakUseCase.recordOnDate(userId, communityId, day3);
        assertThat(resDay3.userCurrentStreak()).isEqualTo(3);
        assertThat(resDay3.communityCurrentStreak()).isEqualTo(3);
        assertThat(resDay3.userLongestStreak()).isEqualTo(3);

        // Verify via API GET /api/v1/users/me/streak and community streak
        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(3))
                .andExpect(jsonPath("$.longestStreak").value(3));

        mockMvc.perform(get("/api/v1/communities/" + communityId + "/streaks/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(3))
                .andExpect(jsonPath("$.longestStreak").value(3));

        // Day 4 was missed. Day 5: Activity performed -> Streak breaks and resets to 1, longest remains 3
        var resDay5 = recordActivityStreakUseCase.recordOnDate(userId, communityId, day5);
        assertThat(resDay5.userCurrentStreak()).isEqualTo(1);
        assertThat(resDay5.communityCurrentStreak()).isEqualTo(1);
        assertThat(resDay5.userLongestStreak()).isEqualTo(3);
        assertThat(resDay5.communityLongestStreak()).isEqualTo(3);

        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.longestStreak").value(3));
    }

    @Test
    @Order(3)
    @DisplayName("Community Free Days: configure free day and preserve streak across free days")
    void shouldConfigureFreeDaysAndPreserveStreak() throws Exception {
        // 2026-11-06 is Friday (5), Saturday 7 is free day, Sunday 8 is free day
        // Owner configures Saturday (6) and Sunday (7) as free days
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/free-days/6")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/communities/" + communityId + "/free-days/7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        // Check free days list
        mockMvc.perform(get("/api/v1/communities/" + communityId + "/free-days")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(6))
                .andExpect(jsonPath("$[1]").value(7));

        // Activity on Friday 2026-11-06 -> streak becomes 2 (since day 5 was 1)
        LocalDate friday = LocalDate.of(2026, 11, 6);
        var resFri = recordActivityStreakUseCase.recordOnDate(userId, communityId, friday);
        assertThat(resFri.communityCurrentStreak()).isEqualTo(2);

        // Saturday and Sunday are free days. Activity on Monday 2026-11-09 -> streak advances to 3!
        LocalDate monday = LocalDate.of(2026, 11, 9);
        var resMon = recordActivityStreakUseCase.recordOnDate(userId, communityId, monday);
        assertThat(resMon.communityCurrentStreak()).isEqualTo(3);

        // Remove Sunday from free days
        mockMvc.perform(delete("/api/v1/communities/" + communityId + "/free-days/7")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/communities/" + communityId + "/free-days")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
