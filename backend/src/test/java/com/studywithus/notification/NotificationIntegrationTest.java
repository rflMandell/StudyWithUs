package com.studywithus.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.notification.presentation.RegisterDeviceRequest;
import com.studywithus.notification.presentation.UpdateNotificationPreferencesRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String tokenUserA;
    private static UUID userAId;

    private static String tokenUserB;
    private static UUID userBId;

    private static UUID communityId;
    private static UUID checkinId;
    private static UUID commentNotificationId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register User A and User B, community and checkin")
    void setupAndSeed() throws Exception {
        // User A (Author of checkin)
        RegisterRequest reqA = new RegisterRequest(
                "user_a_s16",
                "User A Notif",
                "usera_s16@studywithus.com",
                "password123",
                LocalDate.of(1996, 4, 10),
                "USP",
                "Computação",
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

        // User B (Commenter)
        RegisterRequest reqB = new RegisterRequest(
                "user_b_s16",
                "User B Notif",
                "userb_s16@studywithus.com",
                "password123",
                LocalDate.of(1997, 8, 22),
                "UNICAMP",
                "Engenharia",
                "Comentar",
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

        // User A registers device token
        RegisterDeviceRequest deviceReq = new RegisterDeviceRequest("fcm-device-token-user-a", "ANDROID");
        mockMvc.perform(post("/api/v1/devices")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deviceReq)))
                .andExpect(status().isNoContent());

        // User A creates Community (triggers community creator badge notification)
        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Comunidade de Testes de Notificação",
                "Desc",
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

        // User A publishes check-in (triggers first check-in badge notification)
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Estudando arquitetura orientada a eventos por 2 horas!",
                List.of("storage/photo_s16.png")
        );
        MvcResult checkinRes = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CheckinResponse checkinResponse = objectMapper.readValue(checkinRes.getResponse().getContentAsString(), CheckinResponse.class);
        checkinId = checkinResponse.id();

        // Mark setup badge notifications as read so we have a clean slate for test steps
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNoContent());
    }

    @Test
    @Order(2)
    @DisplayName("Estado inicial: Usuário A não possui notificações não-lidas")
    void step1_verifyInitialUnreadCountZero() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        mockMvc.perform(get("/api/v1/notifications?unreadOnly=true")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @Order(3)
    @DisplayName("Cenário Passo 1: Usuário B comenta no check-in de Usuário A e notificação é gerada")
    void step2_userBCommentsOnUserACheckin() throws Exception {
        CreateCommentRequest commentReq = new CreateCommentRequest("Excelente foco, parabéns pelos estudos!");

        mockMvc.perform(post("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated());

        // User A consulta GET /notifications?unreadOnly=true e visualiza alerta
        MvcResult res = mockMvc.perform(get("/api/v1/notifications?unreadOnly=true")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].type").value("NEW_COMMENT"))
                .andExpect(jsonPath("$.content[0].title").value("Novo comentário no seu check-in"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[0].referenceType").value("CHECKIN"))
                .andExpect(jsonPath("$.content[0].referenceId").value(checkinId.toString()))
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        commentNotificationId = UUID.fromString(json.get("content").get(0).get("id").asText());
        assertThat(commentNotificationId).isNotNull();

        // Contador de não lidas é 1
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    @Order(4)
    @DisplayName("Cenário Passo 2: Usuário A marca a notificação como lida")
    void step3_userAMarksNotificationAsRead() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/" + commentNotificationId + "/read")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(commentNotificationId.toString()))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        // Contador de não lidas volta a zero
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));

        // Filtrar unreadOnly retorna vazio
        mockMvc.perform(get("/api/v1/notifications?unreadOnly=true")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @Order(5)
    @DisplayName("Segurança/RBAC: Usuário B não pode alterar a notificação de Usuário A")
    void step4_userBCannotMarkUserANotification() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/" + commentNotificationId + "/read")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @Order(6)
    @DisplayName("Preferências: Usuário desativa notificações de comentários e novas notificações são suprimidas")
    void step5_preferencesDisableCommentNotifications() throws Exception {
        MvcResult initialRes = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andReturn();
        long initialTotal = objectMapper.readTree(initialRes.getResponse().getContentAsString())
                .get("totalElements").asLong();

        UpdateNotificationPreferencesRequest prefReq = new UpdateNotificationPreferencesRequest(
                false, // comments disabled
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prefReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentsEnabled").value(false));

        // User B comments again
        CreateCommentRequest commentReq = new CreateCommentRequest("Outro comentário que deve ser suprimido!");
        mockMvc.perform(post("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated());

        // Total notifications for User A did not increase
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(initialTotal));
    }

    @Test
    @Order(7)
    @DisplayName("Bulk: Marcar todas as notificações como lidas")
    void step6_markAllAsRead() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }
}
