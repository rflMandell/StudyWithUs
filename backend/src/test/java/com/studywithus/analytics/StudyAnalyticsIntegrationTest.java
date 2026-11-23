package com.studywithus.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudyAnalyticsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Sprint 24 End-to-End: Check-ins with subjects/tags/durations feed async aggregates, heatmap, summary, subjects, and reversal on delete")
    void shouldCollectStudyAnalyticsServeSummaryHeatmapSubjectsAndReverseOnDelete() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "an_user_" + suffix,
                                "Analytics Student",
                                "an_" + suffix + "@uni.edu",
                                "pass123456",
                                LocalDate.of(2001, 1, 1),
                                "USP", "Computação", "Meta", "Bio", "dev-an"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(regRes.getResponse().getContentAsString(), AuthResponse.class);

        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                "Comunidade Analytics " + suffix,
                                "Grupo para analytics",
                                null,
                                CommunityVisibility.PUBLIC,
                                "America/Sao_Paulo"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID communityId = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class).id();

        // Check-in 1: Matemática (90 min)
        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                "Estudando Cálculo II",
                                List.of(),
                                "Matemática",
                                List.of("Cálculo", "Integrais"),
                                90
                        ))))
                .andExpect(status().isCreated());

        // Check-in 2: Física (30 min)
        MvcResult c2Res = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                "Revisando Mecânica",
                                List.of(),
                                "Física",
                                List.of("Mecânica"),
                                30
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID c2Id = objectMapper.readValue(c2Res.getResponse().getContentAsString(), CheckinResponse.class).id();

        // Wait for async analytics collector to materialize aggregates
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/analytics/study-summary?days=30")
                                .header("Authorization", "Bearer " + auth.accessToken()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalMinutes").value(120))
                        .andExpect(jsonPath("$.totalHours").value(2.0))
                        .andExpect(jsonPath("$.totalSessions").value(2))
                        .andExpect(jsonPath("$.activeDays").value(1))
                        .andExpect(jsonPath("$.hoursToday").value(2.0))
        );

        // Verify heatmap
        mockMvc.perform(get("/api/v1/analytics/heatmap")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].minutes").value(120))
                .andExpect(jsonPath("$[0].sessionsCount").value(2))
                .andExpect(jsonPath("$[0].intensityLevel").value(4));

        // Verify subject distribution: Matemática (75%) and Física (25%)
        mockMvc.perform(get("/api/v1/analytics/subjects")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Matemática"))
                .andExpect(jsonPath("$[0].percentage").value(75.0))
                .andExpect(jsonPath("$[1].subject").value("Física"))
                .andExpect(jsonPath("$[1].percentage").value(25.0));

        // Delete check-in 2 -> analytics aggregates reverse asynchronously
        mockMvc.perform(delete("/api/v1/checkins/" + c2Id)
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNoContent());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/analytics/study-summary?days=30")
                                .header("Authorization", "Bearer " + auth.accessToken()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalMinutes").value(90))
                        .andExpect(jsonPath("$.totalSessions").value(1))
        );
    }
}
