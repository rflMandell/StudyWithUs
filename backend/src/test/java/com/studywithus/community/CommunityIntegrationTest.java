package com.studywithus.community;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class CommunityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String tokenA;
    private static String tokenB;
    private static UUID communityId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register user A and user B")
    void setup_registerUsers() throws Exception {
        RegisterRequest userARequest = new RegisterRequest(
                "creator_a",
                "Creator Alice",
                "alice_comm@uni.edu",
                "password123",
                LocalDate.of(2000, 1, 1),
                "USP",
                "Computação",
                "Aprender",
                "Bio A",
                "device-a"
        );

        MvcResult resultA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userARequest)))
                .andExpect(status().isCreated())
                .andReturn();
        tokenA = objectMapper.readValue(resultA.getResponse().getContentAsString(), AuthResponse.class).accessToken();

        RegisterRequest userBRequest = new RegisterRequest(
                "student_b",
                "Student Bob",
                "bob_comm@uni.edu",
                "password123",
                LocalDate.of(2000, 2, 2),
                "USP",
                "Computação",
                "Passar",
                "Bio B",
                "device-b"
        );

        MvcResult resultB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        tokenB = objectMapper.readValue(resultB.getResponse().getContentAsString(), AuthResponse.class).accessToken();

        assertThat(tokenA).isNotNull();
        assertThat(tokenB).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("A cria comunidade: User A creates public community 'Clube de Algoritmos'")
    void step2_userACreatesCommunity() throws Exception {
        CreateCommunityRequest request = new CreateCommunityRequest(
                "Clube de Algoritmos",
                "Estudos semanais sobre algoritmos e estruturas de dados",
                "https://images.unsplash.com/photo-community.jpg",
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Clube de Algoritmos"))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
                .andExpect(jsonPath("$.membersCount").value(1))
                .andReturn();

        CommunityResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CommunityResponse.class);
        communityId = response.id();
        assertThat(communityId).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("B visualiza: User B lists and views details of 'Clube de Algoritmos'")
    void step3_userBViewsCommunity() throws Exception {
        // List communities
        mockMvc.perform(get("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenB)
                        .param("search", "Algoritmos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(communityId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Clube de Algoritmos"))
                .andExpect(jsonPath("$.content[0].currentUserRole").doesNotExist());

        // Get details
        mockMvc.perform(get("/api/v1/communities/" + communityId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(communityId.toString()))
                .andExpect(jsonPath("$.name").value("Clube de Algoritmos"))
                .andExpect(jsonPath("$.membersCount").value(1))
                .andExpect(jsonPath("$.currentUserRole").doesNotExist());
    }

    @Test
    @Order(4)
    @DisplayName("B entra: User B joins public community and becomes active MEMBER")
    void step4_userBJoinsCommunity() throws Exception {
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.communityId").value(communityId.toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // User B views details again -> members count is now 2 and currentUserRole is MEMBER
        mockMvc.perform(get("/api/v1/communities/" + communityId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.membersCount").value(2))
                .andExpect(jsonPath("$.currentUserRole").value("MEMBER"));

        // List members endpoint returns 2 members
        mockMvc.perform(get("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @Order(5)
    @DisplayName("B tenta entrar novamente: Conflict 409 COMMUNITY_ALREADY_MEMBER")
    void step5_userBCannotJoinAgain() throws Exception {
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMUNITY_ALREADY_MEMBER"));
    }
}
