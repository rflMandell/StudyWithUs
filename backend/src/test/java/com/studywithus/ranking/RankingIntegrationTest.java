package com.studywithus.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RankingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String tokenA;
    private static UUID userAId;

    private static String tokenB;
    private static UUID userBId;

    private static String tokenC;
    private static UUID userCId;

    private static UUID communityId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register users A, B, C; user A creates community and B, C join")
    void setup() throws Exception {
        tokenA = registerUser("rank_user_a", "Alice Rank", "a_rank@uni.edu");
        userAId = getUserId(tokenA);

        tokenB = registerUser("rank_user_b", "Bob Rank", "b_rank@uni.edu");
        userBId = getUserId(tokenB);

        tokenC = registerUser("rank_user_c", "Charlie Rank", "c_rank@uni.edu");
        userCId = getUserId(tokenC);

        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Olimpíada de Matemática",
                "Comunidade focada em ranking e cálculo",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        MvcResult commResult = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CommunityResponse comm = objectMapper.readValue(commResult.getResponse().getContentAsString(), CommunityResponse.class);
        communityId = comm.id();
        assertThat(communityId).isNotNull();

        // User B joins
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isCreated());

        // User C joins
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("Users create check-ins: A (10 pts), B (20 pts), C (10 pts)")
    void step2_usersCreateCheckins() throws Exception {
        // User A creates 1 check-in (+10 pts)
        createCheckin(tokenA, "Exercício A1");

        // User B creates 2 check-ins (+20 pts)
        createCheckin(tokenB, "Exercício B1");
        createCheckin(tokenB, "Exercício B2");

        // User C creates 1 check-in (+10 pts)
        createCheckin(tokenC, "Exercício C1");
    }

    @Test
    @Order(3)
    @DisplayName("Community ranking lists: 1st Bob (20 pts), 2nd Alice (10 pts), 3rd Charlie (10 pts)")
    void step3_communityRankingOrdered() throws Exception {
        mockMvc.perform(get("/api/v1/communities/" + communityId + "/ranking")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].userId").value(userBId.toString()))
                .andExpect(jsonPath("$.content[0].points").value(20))
                .andExpect(jsonPath("$.content[0].position").value(1))
                .andExpect(jsonPath("$.content[1].points").value(10))
                .andExpect(jsonPath("$.content[1].position").value(2))
                .andExpect(jsonPath("$.content[2].points").value(10))
                .andExpect(jsonPath("$.content[2].position").value(3));
    }

    @Test
    @Order(4)
    @DisplayName("User B checks their own ranking position (1st, 20 pts)")
    void step4_userBChecksRankingMe() throws Exception {
        mockMvc.perform(get("/api/v1/communities/" + communityId + "/ranking/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userBId.toString()))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.points").value(20));
    }

    @Test
    @Order(5)
    @DisplayName("User C checks their own ranking position (3rd, 10 pts)")
    void step5_userCChecksRankingMe() throws Exception {
        mockMvc.perform(get("/api/v1/communities/" + communityId + "/ranking/me")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userCId.toString()))
                .andExpect(jsonPath("$.position").value(3))
                .andExpect(jsonPath("$.points").value(10));
    }

    private String registerUser(String username, String name, String email) throws Exception {
        RegisterRequest req = new RegisterRequest(
                username,
                name,
                email,
                "password123",
                LocalDate.of(2000, 1, 1),
                "USP",
                "Matemática",
                "Medalha",
                "Bio",
                "device-" + username
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class).accessToken();
    }

    private UUID getUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").traverse(objectMapper).readValueAs(UUID.class);
    }

    private void createCheckin(String token, String caption) throws Exception {
        CreateCheckinRequest req = new CreateCheckinRequest(
                communityId,
                caption,
                List.of()
        );

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
