package com.studywithus.challenge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.challenge.domain.ChallengeMetric;
import com.studywithus.challenge.domain.ChallengeScope;
import com.studywithus.challenge.presentation.ChallengeParticipantResponse;
import com.studywithus.challenge.presentation.ChallengeResponse;
import com.studywithus.challenge.presentation.CreateChallengeRequest;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.gamification.domain.BadgeConstants;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
class ChallengeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private static String tokenUserA;
    private static UUID userAId;

    private static String tokenUserB;
    private static UUID userBId;

    private static String tokenUserC;
    private static UUID userCId;

    private static UUID communityId;
    private static UUID challengeId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register users and create community")
    void setupAndSeed() throws Exception {
        // User A (Owner of Community)
        RegisterRequest reqA = new RegisterRequest(
                "owner_chal_s15",
                "Challenge Owner",
                "owner_chal_s15@studywithus.com",
                "password123",
                LocalDate.of(1995, 1, 1),
                "USP",
                "Computação",
                "Criar desafios",
                "Bio A",
                "dev-a"
        );
        MvcResult resA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authA = objectMapper.readValue(resA.getResponse().getContentAsString(), AuthResponse.class);
        tokenUserA = authA.accessToken();
        userAId = authA.userId();

        // User B (Community member & challenger)
        RegisterRequest reqB = new RegisterRequest(
                "member_chal_s15",
                "Challenge Member B",
                "member_chal_s15@studywithus.com",
                "password123",
                LocalDate.of(1998, 3, 15),
                "UNICAMP",
                "Engenharia",
                "Cumprir desafios",
                "Bio B",
                "dev-b"
        );
        MvcResult resB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authB = objectMapper.readValue(resB.getResponse().getContentAsString(), AuthResponse.class);
        tokenUserB = authB.accessToken();
        userBId = authB.userId();

        // User C (Outsider)
        RegisterRequest reqC = new RegisterRequest(
                "outsider_chal_s15",
                "Challenge Outsider C",
                "outsider_chal_s15@studywithus.com",
                "password123",
                LocalDate.of(2000, 5, 20),
                "UFRJ",
                "Física",
                "Apenas espiar",
                "Bio C",
                "dev-c"
        );
        MvcResult resC = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqC)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authC = objectMapper.readValue(resC.getResponse().getContentAsString(), AuthResponse.class);
        tokenUserC = authC.accessToken();
        userCId = authC.userId();

        // User A creates Community
        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Maratona de Estudos Java 2026",
                "Comunidade focada em desafios intensivos de Java",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );
        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CommunityResponse comm = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class);
        communityId = comm.id();

        // User B joins Community
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("Cenário Passo 1: Comunidade cria desafio de 2 check-ins com pontos e badge")
    void step1_communityCreatesChallenge() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest(
                "Desafio 2 Check-ins do Mês",
                "Faça 2 check-ins de estudo na comunidade e ganhe 50 pontos extras e badge!",
                ChallengeScope.INDIVIDUAL,
                ChallengeMetric.CHECKINS,
                2,
                Instant.now().minus(1, ChronoUnit.HOURS),
                Instant.now().plus(30, ChronoUnit.DAYS),
                50,
                BadgeConstants.BADGE_POINTS_100_ID
        );

        MvcResult res = mockMvc.perform(post("/api/v1/communities/" + communityId + "/challenges")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Desafio 2 Check-ins do Mês"))
                .andExpect(jsonPath("$.targetValue").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.rewards").isArray())
                .andExpect(jsonPath("$.rewards.length()").value(2))
                .andReturn();

        ChallengeResponse response = objectMapper.readValue(res.getResponse().getContentAsString(), ChallengeResponse.class);
        challengeId = response.id();
        assertThat(challengeId).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("RBAC: Membro não-administrador e não-membro não podem criar desafios na comunidade")
    void step2_verifyCreateAuthorization() throws Exception {
        CreateChallengeRequest request = new CreateChallengeRequest(
                "Desafio Não Autorizado",
                "Desc",
                ChallengeScope.INDIVIDUAL,
                ChallengeMetric.CHECKINS,
                5,
                null,
                null,
                null,
                null
        );

        // Member B is regular member, not admin
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/challenges")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Outsider C is not even a member
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/challenges")
                        .header("Authorization", "Bearer " + tokenUserC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @Order(4)
    @DisplayName("RBAC: Não-membro não pode se inscrever no desafio comunitário")
    void step3_verifyJoinAuthorization() throws Exception {
        mockMvc.perform(post("/api/v1/challenges/" + challengeId + "/join")
                        .header("Authorization", "Bearer " + tokenUserC))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMUNITY_NOT_A_MEMBER"));
    }

    @Test
    @Order(5)
    @DisplayName("Cenário Passo 2: Usuário B se inscreve no desafio")
    void step4_userBJoinsChallenge() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/challenges/" + challengeId + "/join")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challengeId").value(challengeId.toString()))
                .andExpect(jsonPath("$.userId").value(userBId.toString()))
                .andExpect(jsonPath("$.progress").value(0))
                .andExpect(jsonPath("$.targetValue").value(2))
                .andExpect(jsonPath("$.percentage").value(0.0))
                .andExpect(jsonPath("$.completed").value(false))
                .andReturn();

        ChallengeParticipantResponse participant = objectMapper.readValue(
                res.getResponse().getContentAsString(),
                ChallengeParticipantResponse.class
        );
        assertThat(participant.completed()).isFalse();

        // Idempotency: joining again returns the same participant state
        mockMvc.perform(post("/api/v1/challenges/" + challengeId + "/join")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(0));
    }

    @Test
    @Order(6)
    @DisplayName("Cenário Passo 3: Usuário B realiza o primeiro check-in e progresso avança para 50%")
    void step5_userBFirstCheckinAdvancesProgress() throws Exception {
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Check-in #1 para o Desafio: Estudei JPA e Flyway!",
                List.of("storage/photo_challenge_1.png")
        );

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated());

        // Consult progress
        mockMvc.perform(get("/api/v1/challenges/" + challengeId + "/progress/me")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(1))
                .andExpect(jsonPath("$.targetValue").value(2))
                .andExpect(jsonPath("$.percentage").value(50.0))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @Order(7)
    @DisplayName("Cenário Passo 4: Usuário B realiza o segundo check-in e desafio é completado (100%)")
    void step6_userBSecondCheckinCompletesChallenge() throws Exception {
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Check-in #2 para o Desafio: Desafio cumprido com sucesso!",
                List.of("storage/photo_challenge_2.png")
        );

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated());

        // Consult progress: completed!
        mockMvc.perform(get("/api/v1/challenges/" + challengeId + "/progress/me")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progress").value(2))
                .andExpect(jsonPath("$.targetValue").value(2))
                .andExpect(jsonPath("$.percentage").value(100.0))
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("Cenário Passo 5: Validar que pontos extras (+50 pts) e badge foram creditados")
    void step7_verifyRewardsCredited() throws Exception {
        // Verify User B's lifetime points increased
        User userB = userRepository.findById(userBId).orElseThrow();
        // Base points per checkin (e.g. 10 per checkin * 2 = 20) + 50 challenge points >= 70
        assertThat(userB.getLifetimePoints()).isGreaterThanOrEqualTo(70);

        // Verify badge is listed for User B
        MvcResult badgeRes = mockMvc.perform(get("/api/v1/users/" + userBId + "/badges")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode badges = objectMapper.readTree(badgeRes.getResponse().getContentAsString());
        assertThat(badges.isArray()).isTrue();

        boolean hasChallengeBadge = false;
        for (JsonNode b : badges) {
            if (b.get("badgeId").asText().equals(BadgeConstants.BADGE_POINTS_100_ID.toString())) {
                hasChallengeBadge = true;
                break;
            }
        }
        assertThat(hasChallengeBadge).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("Listing: List community challenges")
    void step8_listCommunityChallenges() throws Exception {
        mockMvc.perform(get("/api/v1/communities/" + communityId + "/challenges")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(challengeId.toString()))
                .andExpect(jsonPath("$[0].title").value("Desafio 2 Check-ins do Mês"));
    }
}
