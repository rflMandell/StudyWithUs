package com.studywithus.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.social.presentation.CommentResponse;
import com.studywithus.social.presentation.CreateCommentRequest;
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
class CommentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String tokenUserA;
    private static UUID userAId;

    private static String tokenUserB;
    private static UUID userBId;

    private static String tokenUserC;
    private static UUID userCId;

    private static UUID communityId;
    private static UUID checkinId;
    private static UUID commentId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register User A (owner), User B (member), User C (non-member) and create community")
    void setup() throws Exception {
        // User A
        RegisterRequest reqA = new RegisterRequest(
                "user_a_sprint13",
                "User A Author",
                "usera_s13@studywithus.com",
                "password123",
                LocalDate.of(2000, 1, 10),
                "USP",
                "Engenharia",
                "Aprender",
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

        // User B
        RegisterRequest reqB = new RegisterRequest(
                "user_b_sprint13",
                "User B Member",
                "userb_s13@studywithus.com",
                "password123",
                LocalDate.of(2001, 2, 15),
                "Unicamp",
                "Computação",
                "Estudar em grupo",
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

        // User C (Non-member)
        RegisterRequest reqC = new RegisterRequest(
                "user_c_sprint13",
                "User C Outsider",
                "userc_s13@studywithus.com",
                "password123",
                LocalDate.of(1999, 5, 20),
                "UFMG",
                "Matemática",
                "Observar",
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

        // User A creates community
        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Clube de Algoritmos Avançados",
                "Comunidade para resolução de problemas",
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

        // User B joins community
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("Cenário Passo 1: Usuário A publica check-in")
    void step1_userAPublishesCheckin() throws Exception {
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Hoje estudei grafos e caminhos mínimos com Dijkstra por 2h!",
                List.of("storage/photo1.png")
        );

        MvcResult res = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(userAId.toString()))
                .andExpect(jsonPath("$.commentsCount").value(0))
                .andReturn();

        CheckinResponse checkinResponse = objectMapper.readValue(res.getResponse().getContentAsString(), CheckinResponse.class);
        checkinId = checkinResponse.id();
        assertThat(checkinId).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("Cenário Passo 2: Usuário B comenta no check-in")
    void step2_userBCommentsOnCheckin() throws Exception {
        CreateCommentRequest commentReq = new CreateCommentRequest("Excelente foco, parabéns pela dedicação!");

        MvcResult res = mockMvc.perform(post("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.checkinId").value(checkinId.toString()))
                .andExpect(jsonPath("$.author.id").value(userBId.toString()))
                .andExpect(jsonPath("$.author.username").value("user_b_sprint13"))
                .andExpect(jsonPath("$.content").value("Excelente foco, parabéns pela dedicação!"))
                .andReturn();

        CommentResponse commentResponse = objectMapper.readValue(res.getResponse().getContentAsString(), CommentResponse.class);
        commentId = commentResponse.id();
        assertThat(commentId).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("Cenário Passo 3: Comentário listado no check-in e contador incrementado no feed")
    void step3_commentListedInCheckinAndFeedReflectsCount() throws Exception {
        // List comments for checkin
        mockMvc.perform(get("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.content[0].author.username").value("user_b_sprint13"))
                .andExpect(jsonPath("$.content[0].content").value("Excelente foco, parabéns pela dedicação!"));

        // Check feed for User A
        MvcResult feedRes = mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode feedNode = objectMapper.readTree(feedRes.getResponse().getContentAsString());
        JsonNode items = feedNode.get("content");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThanOrEqualTo(1);

        JsonNode checkinItem = null;
        for (JsonNode item : items) {
            if (item.get("id").asText().equals(checkinId.toString())) {
                checkinItem = item;
                break;
            }
        }
        assertThat(checkinItem).isNotNull();
        assertThat(checkinItem.get("commentsCount").asLong()).isEqualTo(1L);
    }

    @Test
    @Order(5)
    @DisplayName("Cenário Passo 4: Usuário C (não membro) tenta comentar e recebe 403 Forbidden")
    void step4_userCNonMemberCannotComment() throws Exception {
        CreateCommentRequest commentReq = new CreateCommentRequest("Tentativa de comentar sem ser membro da comunidade.");

        mockMvc.perform(post("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserC)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMUNITY_NOT_A_MEMBER"));
    }

    @Test
    @Order(6)
    @DisplayName("Proteção: Usuário A não pode remover comentário do Usuário B (403 Forbidden)")
    void step5_nonAuthorCannotDeleteComment() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"));
    }

    @Test
    @Order(7)
    @DisplayName("Cenário Passo 5: Usuário B remove seu próprio comentário com sucesso")
    void step6_userBRemovesOwnComment() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/" + commentId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNoContent());

        // Now list comments should be empty
        mockMvc.perform(get("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());

        // Feed commentsCount should now be 0
        MvcResult feedRes = mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode feedNode = objectMapper.readTree(feedRes.getResponse().getContentAsString());
        for (JsonNode item : feedNode.get("content")) {
            if (item.get("id").asText().equals(checkinId.toString())) {
                assertThat(item.get("commentsCount").asLong()).isEqualTo(0L);
            }
        }
    }
}
