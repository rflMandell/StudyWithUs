package com.studywithus.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.identity.presentation.UpdateUserProfileRequest;
import com.studywithus.identity.presentation.UserProfileResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProfileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String aliceToken;
    private static UUID aliceId;
    private static String bobToken;
    private static UUID bobId;
    private static UUID communityId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register Alice and Bob, Alice creates community")
    void setup() throws Exception {
        // Register Alice
        MvcResult resAlice = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "profile_alice",
                                "Alice Original",
                                "profile_alice@uni.edu",
                                "pass123456",
                                LocalDate.of(2000, 1, 1),
                                "USP", "Direito", "Aprender", "Bio Inicial", "dev-alice"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authAlice = objectMapper.readValue(resAlice.getResponse().getContentAsString(), AuthResponse.class);
        aliceToken = authAlice.accessToken();
        aliceId = authAlice.userId();

        // Register Bob
        MvcResult resBob = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "profile_bob",
                                "Bob Aluno",
                                "profile_bob@uni.edu",
                                "pass123456",
                                LocalDate.of(2001, 2, 2),
                                "Unicamp", "Medicina", "Meta", "Bio", "dev-bob"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authBob = objectMapper.readValue(resBob.getResponse().getContentAsString(), AuthResponse.class);
        bobToken = authBob.accessToken();
        bobId = authBob.userId();

        // Alice creates community
        MvcResult resComm = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                "Comunidade Perfil Teste",
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
    @DisplayName("Demonstrable test: Alice performs checkin -> profile reflects stats (points, communities, checkins)")
    void shouldReflectActivityInUserProfile() throws Exception {
        // Alice posts checkin (+10 pts)
        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                "Check-in de teste para refletir no perfil",
                                List.of()
                        ))))
                .andExpect(status().isCreated());

        // Alice gets her profile
        MvcResult resProfile = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceId.toString()))
                .andExpect(jsonPath("$.username").value("profile_alice"))
                .andExpect(jsonPath("$.name").value("Alice Original"))
                .andExpect(jsonPath("$.currentPoints").value(10))
                .andExpect(jsonPath("$.activeCommunitiesCount").value(1))
                .andExpect(jsonPath("$.checkinsCount").value(1))
                .andReturn();

        UserProfileResponse profile = objectMapper.readValue(resProfile.getResponse().getContentAsString(), UserProfileResponse.class);
        assertThat(profile.currentPoints()).isEqualTo(10);
        assertThat(profile.activeCommunitiesCount()).isEqualTo(1);
        assertThat(profile.checkinsCount()).isEqualTo(1);
    }

    @Test
    @Order(3)
    @DisplayName("Update profile test: Alice updates profile via PATCH /users/me -> reflected in me and public view")
    void shouldUpdateProfileAndReflectInPublicView() throws Exception {
        UpdateUserProfileRequest updateReq = new UpdateUserProfileRequest(
                "Alice Atualizada",
                "Nova Bio Científica",
                "USP Ribeirão",
                "Biotecnologia",
                "Concluir mestrado",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb",
                LocalDate.of(2000, 5, 15)
        );

        // Alice patches her profile
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Atualizada"))
                .andExpect(jsonPath("$.bio").value("Nova Bio Científica"))
                .andExpect(jsonPath("$.institution").value("USP Ribeirão"))
                .andExpect(jsonPath("$.course").value("Biotecnologia"))
                .andExpect(jsonPath("$.studyGoal").value("Concluir mestrado"))
                .andExpect(jsonPath("$.avatarUrl").value("https://images.unsplash.com/photo-1534528741775-53994a69daeb"));

        // Bob accesses Alice's profile by ID
        mockMvc.perform(get("/api/v1/users/" + aliceId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Atualizada"))
                .andExpect(jsonPath("$.bio").value("Nova Bio Científica"))
                .andExpect(jsonPath("$.institution").value("USP Ribeirão"))
                .andExpect(jsonPath("$.course").value("Biotecnologia"))
                .andExpect(jsonPath("$.currentPoints").value(10))
                .andExpect(jsonPath("$.checkinsCount").value(1));
    }

    @Test
    @Order(4)
    @DisplayName("Streak & Badges endpoints: user can query streak and badges")
    void shouldQueryStreakAndBadges() throws Exception {
        // Query streak
        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(aliceId.toString()))
                .andExpect(jsonPath("$.currentStreak").isNumber())
                .andExpect(jsonPath("$.longestStreak").isNumber());

        // Query badges
        mockMvc.perform(get("/api/v1/users/" + aliceId + "/badges")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
