package com.studywithus.gamification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.gamification.domain.BadgeConstants;
import com.studywithus.gamification.domain.StreakUpdatedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BadgeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private static String userToken;
    private static UUID userId;
    private static UUID communityId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register user and verify available badges catalog")
    void setupAndVerifyCatalog() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "gamification_hero",
                "Hero Gamification",
                "hero_badges@studywithus.com",
                "password123",
                LocalDate.of(2001, 4, 12),
                "Unicamp",
                "Engenharia",
                "Conquistar todas as badges",
                "Bio",
                "dev-hero"
        );

        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
        userToken = auth.accessToken();
        userId = auth.userId();

        // Check catalog of available badges
        MvcResult catalogRes = mockMvc.perform(get("/api/v1/badges")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode catalogNode = objectMapper.readTree(catalogRes.getResponse().getContentAsString());
        assertThat(catalogNode.isArray()).isTrue();
        assertThat(catalogNode.size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @Order(2)
    @DisplayName("Cenário: Criar comunidade concede badge de Criador de Comunidade")
    void shouldAwardCommunityCreatorBadge() throws Exception {
        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Comunidade Gamificada",
                "Grupo focado em conquistas",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CommunityResponse comm = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class);
        communityId = comm.id();

        // Check user earned badges
        MvcResult badgesRes = mockMvc.perform(get("/api/v1/users/" + userId + "/badges")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode badges = objectMapper.readTree(badgesRes.getResponse().getContentAsString());
        assertThat(badges.isArray()).isTrue();
        boolean foundCreator = false;
        for (JsonNode b : badges) {
            if (b.get("name").asText().equals(BadgeConstants.BADGE_COMMUNITY_CREATOR_NAME)) {
                foundCreator = true;
                break;
            }
        }
        assertThat(foundCreator).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Cenário: Realizar primeiro check-in concede badge 'Primeiro Check-in'")
    void shouldAwardFirstCheckinBadge() throws Exception {
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Primeiro check-in de estudos no grupo!",
                Collections.emptyList()
        );

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated());

        // Check user earned badges
        MvcResult badgesRes = mockMvc.perform(get("/api/v1/users/" + userId + "/badges")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode badges = objectMapper.readTree(badgesRes.getResponse().getContentAsString());
        boolean foundFirstCheckin = false;
        for (JsonNode b : badges) {
            if (b.get("name").asText().equals(BadgeConstants.BADGE_FIRST_CHECKIN_NAME)) {
                foundFirstCheckin = true;
                break;
            }
        }
        assertThat(foundFirstCheckin).isTrue();
    }

    @Test
    @Order(4)
    @DisplayName("Cenário Roadmap Sprint 14: Usuário atinge marco de 7 dias de streak -> StreakUpdatedEvent dispara BadgeEvaluator -> user_badges recebe conquista")
    void shouldAward7DayStreakBadgeOnStreakUpdatedEvent() throws Exception {
        // Usuário atinge marco (7 dias de streak)
        // Dispara StreakUpdatedEvent
        eventPublisher.publishEvent(new StreakUpdatedEvent(
                userId,
                communityId,
                7,
                7,
                LocalDate.now()
        ));

        // GET /users/{userId}/badges lista a conquista
        MvcResult badgesRes = mockMvc.perform(get("/api/v1/users/" + userId + "/badges")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode badges = objectMapper.readTree(badgesRes.getResponse().getContentAsString());
        boolean foundStreak7 = false;
        for (JsonNode b : badges) {
            if (b.get("name").asText().equals(BadgeConstants.BADGE_STREAK_7_NAME)) {
                foundStreak7 = true;
                assertThat(b.get("badgeId").asText()).isEqualTo(BadgeConstants.BADGE_STREAK_7_ID.toString());
                assertThat(b.get("description").asText()).contains("7 dias consecutivos");
                break;
            }
        }
        assertThat(foundStreak7).isTrue();
    }

    @Test
    @Order(5)
    @DisplayName("Idempotência: Disparar evento novamente não duplica badge na lista do usuário")
    void shouldBeIdempotentOnSubsequentEvents() throws Exception {
        // Disparar novamente marco de 7 dias
        eventPublisher.publishEvent(new StreakUpdatedEvent(
                userId,
                communityId,
                7,
                7,
                LocalDate.now()
        ));

        MvcResult badgesRes = mockMvc.perform(get("/api/v1/users/" + userId + "/badges")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode badges = objectMapper.readTree(badgesRes.getResponse().getContentAsString());
        int countStreak7 = 0;
        for (JsonNode b : badges) {
            if (b.get("name").asText().equals(BadgeConstants.BADGE_STREAK_7_NAME)) {
                countStreak7++;
            }
        }
        assertThat(countStreak7).isEqualTo(1);
    }
}
