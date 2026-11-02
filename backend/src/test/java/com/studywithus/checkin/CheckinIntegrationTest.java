package com.studywithus.checkin;

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
class CheckinIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String authorToken;
    private static UUID authorId;
    private static String nonMemberToken;
    private static UUID communityId;
    private static UUID createdCheckinId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register author and non-member users, author creates community")
    void setup() throws Exception {
        RegisterRequest authorReq = new RegisterRequest(
                "checkin_author",
                "Author Student",
                "author_checkin@uni.edu",
                "password123",
                LocalDate.of(2000, 3, 15),
                "USP",
                "Física",
                "Estudar",
                "Bio",
                "device-auth"
        );

        MvcResult resultAuthor = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authorReq)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(resultAuthor.getResponse().getContentAsString(), AuthResponse.class);
        authorToken = auth.accessToken();
        authorId = auth.userId();

        RegisterRequest nonMemberReq = new RegisterRequest(
                "non_member_user",
                "Non Member",
                "non_member@uni.edu",
                "password123",
                LocalDate.of(2002, 7, 22),
                "USP",
                "Física",
                "Observar",
                "Bio",
                "device-non"
        );

        MvcResult resultNonMember = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nonMemberReq)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse nonAuth = objectMapper.readValue(resultNonMember.getResponse().getContentAsString(), AuthResponse.class);
        nonMemberToken = nonAuth.accessToken();

        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Grupo de Termodinâmica",
                "Resoluções e checkins diários",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        MvcResult commResult = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + authorToken)
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
    @DisplayName("Non-member attempting to create checkin receives 403 COMMUNITY_NOT_A_MEMBER")
    void step2_nonMemberCannotCreateCheckin() throws Exception {
        CreateCheckinRequest request = new CreateCheckinRequest(
                communityId,
                "Tentativa de check-in sem ser membro",
                List.of()
        );

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + nonMemberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMUNITY_NOT_A_MEMBER"));
    }

    @Test
    @Order(3)
    @DisplayName("Author (active member) creates check-in with Idempotency-Key (201 Created)")
    void step3_authorCreatesCheckin() throws Exception {
        CreateCheckinRequest request = new CreateCheckinRequest(
                communityId,
                "Capítulo 4 - Entropia finalizado com 10 exercícios",
                List.of("uploads/termo_ex1.png")
        );

        MvcResult result = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "idem-key-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caption").value("Capítulo 4 - Entropia finalizado com 10 exercícios"))
                .andExpect(jsonPath("$.userId").value(authorId.toString()))
                .andExpect(jsonPath("$.mediaKeys[0]").value("uploads/termo_ex1.png"))
                .andReturn();

        CheckinResponse checkin = objectMapper.readValue(result.getResponse().getContentAsString(), CheckinResponse.class);
        createdCheckinId = checkin.id();
        assertThat(createdCheckinId).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("Author sends identical request with same Idempotency-Key and gets same checkin")
    void step4_idempotentRequestReturnsSameCheckin() throws Exception {
        CreateCheckinRequest request = new CreateCheckinRequest(
                communityId,
                "Capítulo 4 - Entropia finalizado com 10 exercícios",
                List.of("uploads/termo_ex1.png")
        );

        MvcResult result = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + authorToken)
                        .header("Idempotency-Key", "idem-key-100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(createdCheckinId.toString()))
                .andReturn();

        CheckinResponse checkin = objectMapper.readValue(result.getResponse().getContentAsString(), CheckinResponse.class);
        assertThat(checkin.id()).isEqualTo(createdCheckinId);
    }

    @Test
    @Order(5)
    @DisplayName("Get checkin details by ID returns 200 OK")
    void step5_getCheckinDetails() throws Exception {
        mockMvc.perform(get("/api/v1/checkins/" + createdCheckinId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdCheckinId.toString()))
                .andExpect(jsonPath("$.caption").value("Capítulo 4 - Entropia finalizado com 10 exercícios"));
    }

    @Test
    @Order(6)
    @DisplayName("List community checkins includes the newly created checkin")
    void step6_listCommunityCheckins() throws Exception {
        mockMvc.perform(get("/api/v1/communities/" + communityId + "/checkins")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(createdCheckinId.toString()));
    }

    @Test
    @Order(7)
    @DisplayName("Non-author attempting to delete checkin receives 403 CHECKIN_ACCESS_DENIED")
    void step7_nonAuthorCannotDeleteCheckin() throws Exception {
        mockMvc.perform(delete("/api/v1/checkins/" + createdCheckinId)
                        .header("Authorization", "Bearer " + nonMemberToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHECKIN_ACCESS_DENIED"));
    }

    @Test
    @Order(8)
    @DisplayName("Author deletes own checkin (204 No Content)")
    void step8_authorDeletesOwnCheckin() throws Exception {
        mockMvc.perform(delete("/api/v1/checkins/" + createdCheckinId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(9)
    @DisplayName("Get checkin details after deletion returns 404 CHECKIN_NOT_FOUND")
    void step9_getDeletedCheckinReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/checkins/" + createdCheckinId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CHECKIN_NOT_FOUND"));
    }
}
