package com.studywithus.gamification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
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
import java.util.List;
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
class PointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String token;
    private static UUID userId;
    private static UUID communityId;
    private static UUID checkinId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register user and create community")
    void setup() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "points_student",
                "Points Student",
                "points_student@uni.edu",
                "password123",
                LocalDate.of(2000, 4, 10),
                "USP",
                "Estatística",
                "Aprender",
                "Bio",
                "device-points"
        );

        MvcResult authResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(authResult.getResponse().getContentAsString(), AuthResponse.class);
        token = auth.accessToken();
        userId = auth.userId();

        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Clube de Probabilidade",
                "Estudos de processos estocásticos",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        MvcResult commResult = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CommunityResponse comm = objectMapper.readValue(commResult.getResponse().getContentAsString(), CommunityResponse.class);
        communityId = comm.id();
        assertThat(communityId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Initial points summary is zero")
    void step2_initialPointsIsZero() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/points")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.currentPoints").value(0))
                .andExpect(jsonPath("$.lifetimePoints").value(0));
    }

    @Test
    @Order(3)
    @DisplayName("Checkin created triggers automatic points award (+10 points)")
    void step3_checkinAwardsPoints() throws Exception {
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Estudei Cadeias de Markov por 1 hora",
                List.of()
        );

        MvcResult checkinResult = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CheckinResponse checkin = objectMapper.readValue(checkinResult.getResponse().getContentAsString(), CheckinResponse.class);
        checkinId = checkin.id();
        assertThat(checkinId).isNotNull();

        // Verify points summary updated to 10
        mockMvc.perform(get("/api/v1/users/me/points")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(10))
                .andExpect(jsonPath("$.lifetimePoints").value(10));

        // Verify points history contains CHECKIN transaction
        mockMvc.perform(get("/api/v1/users/me/points/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("CHECKIN"))
                .andExpect(jsonPath("$.content[0].amount").value(10))
                .andExpect(jsonPath("$.content[0].checkinId").value(checkinId.toString()));
    }

    @Test
    @Order(4)
    @DisplayName("Checkin deleted triggers automatic reversal transaction (-10 points)")
    void step4_checkinDeletedReversesPoints() throws Exception {
        mockMvc.perform(delete("/api/v1/checkins/" + checkinId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify points summary rolled back to 0
        mockMvc.perform(get("/api/v1/users/me/points")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPoints").value(0))
                .andExpect(jsonPath("$.lifetimePoints").value(0));

        // Verify points history now contains 2 transactions (REVERSAL and CHECKIN)
        mockMvc.perform(get("/api/v1/users/me/points/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].type").value("REVERSAL"))
                .andExpect(jsonPath("$.content[0].amount").value(-10));
    }
}
