package com.studywithus.community;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.community.domain.CommunityRole;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityJoinRequestResponse;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.community.presentation.UpdateMemberRoleRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CommunityManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String ownerToken;
    private static UUID ownerId;
    private static String candidateToken;
    private static UUID candidateId;
    private static UUID privateCommunityId;
    private static UUID joinRequestId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register Owner and Candidate users")
    void setup_registerUsers() throws Exception {
        RegisterRequest ownerReq = new RegisterRequest(
                "owner_mgmt",
                "Owner Management",
                "owner_mgmt@uni.edu",
                "password123",
                LocalDate.of(1999, 5, 10),
                "USP",
                "Engenharia",
                "Liderar",
                "Owner bio",
                "device-owner"
        );

        MvcResult resultOwner = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ownerReq)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authOwner = objectMapper.readValue(resultOwner.getResponse().getContentAsString(), AuthResponse.class);
        ownerToken = authOwner.accessToken();
        ownerId = authOwner.userId();

        RegisterRequest candidateReq = new RegisterRequest(
                "candidate_mgmt",
                "Candidate Management",
                "candidate_mgmt@uni.edu",
                "password123",
                LocalDate.of(2001, 8, 20),
                "USP",
                "Engenharia",
                "Participar",
                "Candidate bio",
                "device-candidate"
        );

        MvcResult resultCandidate = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(candidateReq)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authCandidate = objectMapper.readValue(resultCandidate.getResponse().getContentAsString(), AuthResponse.class);
        candidateToken = authCandidate.accessToken();
        candidateId = authCandidate.userId();

        assertThat(ownerToken).isNotNull();
        assertThat(candidateToken).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Owner creates PRIVATE community")
    void step2_ownerCreatesPrivateCommunity() throws Exception {
        CreateCommunityRequest request = new CreateCommunityRequest(
                "Laboratório Avançado",
                "Comunidade privada de pesquisa",
                null,
                CommunityVisibility.PRIVATE,
                "America/Sao_Paulo"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andReturn();

        CommunityResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CommunityResponse.class);
        privateCommunityId = response.id();
        assertThat(privateCommunityId).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("Candidate direct join to private community fails with 400")
    void step3_directJoinPrivateCommunityFails() throws Exception {
        mockMvc.perform(post("/api/v1/communities/" + privateCommunityId + "/members")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMUNITY_PRIVATE_REQUIRES_JOIN_REQUEST"));
    }

    @Test
    @Order(4)
    @DisplayName("Candidate requests to join private community (201 PENDING)")
    void step4_candidateRequestsJoin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/communities/" + privateCommunityId + "/join-requests")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.userId").value(candidateId.toString()))
                .andReturn();

        CommunityJoinRequestResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CommunityJoinRequestResponse.class
        );
        joinRequestId = response.id();
        assertThat(joinRequestId).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("Candidate sending duplicate join request receives 409 CONFLICT")
    void step5_candidateDuplicateJoinRequestFails() throws Exception {
        mockMvc.perform(post("/api/v1/communities/" + privateCommunityId + "/join-requests")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOIN_REQUEST_ALREADY_PENDING"));
    }

    @Test
    @Order(6)
    @DisplayName("Candidate (non-member) listing join requests receives 403 FORBIDDEN")
    void step6_candidateCannotListJoinRequests() throws Exception {
        mockMvc.perform(get("/api/v1/communities/" + privateCommunityId + "/join-requests")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMUNITY_ACCESS_DENIED"));
    }

    @Test
    @Order(7)
    @DisplayName("Owner lists join requests and sees Candidate's pending request")
    void step7_ownerListsJoinRequests() throws Exception {
        mockMvc.perform(get("/api/v1/communities/" + privateCommunityId + "/join-requests")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(joinRequestId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    @Order(8)
    @DisplayName("Owner approves join request: Candidate becomes active member")
    void step8_ownerApprovesJoinRequest() throws Exception {
        mockMvc.perform(post("/api/v1/communities/" + privateCommunityId + "/join-requests/" + joinRequestId + "/approve")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value(ownerId.toString()));

        // Verify Candidate is now an active member
        mockMvc.perform(get("/api/v1/communities/" + privateCommunityId + "/members")
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @Order(9)
    @DisplayName("Owner promotes Candidate to ADMIN")
    void step9_ownerPromotesCandidateToAdmin() throws Exception {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(CommunityRole.ADMIN);

        mockMvc.perform(put("/api/v1/communities/" + privateCommunityId + "/members/" + candidateId + "/role")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @Order(10)
    @DisplayName("Candidate (now Admin) trying to remove Owner receives 403 FORBIDDEN")
    void step10_adminCannotRemoveOwner() throws Exception {
        mockMvc.perform(delete("/api/v1/communities/" + privateCommunityId + "/members/" + ownerId)
                        .header("Authorization", "Bearer " + candidateToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CANNOT_REMOVE_OWNER"));
    }

    @Test
    @Order(11)
    @DisplayName("Owner demotes Candidate back to MEMBER")
    void step11_ownerDemotesCandidateToMember() throws Exception {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest(CommunityRole.MEMBER);

        mockMvc.perform(put("/api/v1/communities/" + privateCommunityId + "/members/" + candidateId + "/role")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    @Order(12)
    @DisplayName("Owner removes Candidate from community (204 No Content)")
    void step12_ownerRemovesCandidate() throws Exception {
        mockMvc.perform(delete("/api/v1/communities/" + privateCommunityId + "/members/" + candidateId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        // Verify member count decreased
        mockMvc.perform(get("/api/v1/communities/" + privateCommunityId + "/members")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
