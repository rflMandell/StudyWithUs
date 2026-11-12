package com.studywithus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.moderation.domain.ModerationAction;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.moderation.presentation.CreateReportRequest;
import com.studywithus.moderation.presentation.ReportResponse;
import com.studywithus.moderation.presentation.ResolveReportRequest;
import com.studywithus.social.presentation.CommentResponse;
import com.studywithus.social.presentation.CreateCommentRequest;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.presentation.InitiateSubscriptionRequest;
import com.studywithus.subscription.presentation.InitiateSubscriptionResponse;
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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Phase2FullFlowIntegrationTest {

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
    private static UUID normalCommentId;
    private static UUID abusiveCommentId;
    private static UUID reportId;
    private static String checkoutSessionId;

    @Test
    @Order(1)
    @DisplayName("Jornada Passo 1: Cadastro dos Usuários A e B")
    void step1_registerUsers() throws Exception {
        RegisterRequest reqA = new RegisterRequest(
                "user_phase2_a",
                "Alice Phase 2",
                "alice_p2@studywithus.com",
                "password123",
                LocalDate.of(1996, 7, 20),
                "USP",
                "Engenharia",
                "Estudar com foco",
                "Bio Alice",
                "alice-p2"
        );
        MvcResult resA = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authA = objectMapper.readValue(resA.getResponse().getContentAsString(), AuthResponse.class);
        tokenUserA = authA.accessToken();
        userAId = authA.userId();

        RegisterRequest reqB = new RegisterRequest(
                "user_phase2_b",
                "Bob Phase 2",
                "bob_p2@studywithus.com",
                "password123",
                LocalDate.of(1998, 9, 10),
                "Unicamp",
                "Computação",
                "Aprender juntos",
                "Bio Bob",
                "bob-p2"
        );
        MvcResult resB = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authB = objectMapper.readValue(resB.getResponse().getContentAsString(), AuthResponse.class);
        tokenUserB = authB.accessToken();
        userBId = authB.userId();

        assertThat(tokenUserA).isNotNull();
        assertThat(tokenUserB).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Jornada Passo 2: Usuário A cria 1ª Comunidade e recebe Badge 'Criador de Comunidade'")
    void step2_createFirstCommunityAndBadge() throws Exception {
        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Fase 2 Comunidade de Algoritmos",
                "Comunidade para estudo conjunto e resolução de desafios",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        MvcResult res = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        CommunityResponse commResp = objectMapper.readValue(res.getResponse().getContentAsString(), CommunityResponse.class);
        communityId = commResp.id();

        // Usuário B entra na comunidade
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isCreated());

        // Verifica badge concedido
        mockMvc.perform(get("/api/v1/users/" + userAId + "/badges")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Criador de Comunidade')]").exists());
    }

    @Test
    @Order(3)
    @DisplayName("Jornada Passo 3: Usuário A publica Check-in e ganha badge 'Primeiro Check-in' com Notificação")
    void step3_publishCheckinAndNotification() throws Exception {
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Estudei Grafos e Algoritmo de Kruskal por 2 horas!",
                List.of("storage/graph1.png")
        );

        MvcResult res = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CheckinResponse chkResp = objectMapper.readValue(res.getResponse().getContentAsString(), CheckinResponse.class);
        checkinId = chkResp.id();

        // Verifica notificação in-app gerada para o Usuário A
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.type == 'BADGE_GRANTED')]").exists());
    }

    @Test
    @Order(4)
    @DisplayName("Jornada Passo 4: Usuário B comenta no check-in e Usuário A recebe notificação NEW_COMMENT")
    void step4_socialCommentAndNotification() throws Exception {
        CreateCommentRequest commentReq = new CreateCommentRequest(
                "Excelente resumo, Kruskal é muito eficiente para MST!"
        );

        MvcResult commentRes = mockMvc.perform(post("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andReturn();

        CommentResponse commentResp = objectMapper.readValue(commentRes.getResponse().getContentAsString(), CommentResponse.class);
        normalCommentId = commentResp.id();

        // Notificação NEW_COMMENT para Usuário A
        MvcResult notifRes = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.type == 'NEW_COMMENT')]").exists())
                .andReturn();

        JsonNode root = objectMapper.readTree(notifRes.getResponse().getContentAsString());
        UUID notifId = UUID.fromString(root.get("content").get(0).get("id").asText());

        // Marca notificação como lida
        mockMvc.perform(patch("/api/v1/notifications/" + notifId + "/read")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @Order(5)
    @DisplayName("Jornada Passo 5: Moderação - Usuário A denuncia comentário spam e moderador remove com AuditLog")
    void step5_moderationAndAuditLog() throws Exception {
        // Usuário B posta comentário spam
        CreateCommentRequest spamReq = new CreateCommentRequest(
                "COMPRE CURSO PIRATA AQUI: http://link-malicioso.fake"
        );
        MvcResult spamRes = mockMvc.perform(post("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(spamReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CommentResponse spamResp = objectMapper.readValue(spamRes.getResponse().getContentAsString(), CommentResponse.class);
        abusiveCommentId = spamResp.id();

        // Usuário A reporta
        CreateReportRequest reportReq = new CreateReportRequest(
                ReportTargetType.COMMENT,
                abusiveCommentId,
                "Spam e links externos maliciosos",
                "Divulgação indevida de conteúdo pirata"
        );
        MvcResult repRes = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        ReportResponse repResp = objectMapper.readValue(repRes.getResponse().getContentAsString(), ReportResponse.class);
        reportId = repResp.id();

        // Moderador (Usuário A, dono da comunidade) resolve a denúncia
        ResolveReportRequest resolveReq = new ResolveReportRequest(
                ModerationAction.REMOVE_CONTENT,
                "Conteúdo removido conforme as diretrizes da comunidade"
        );
        mockMvc.perform(post("/api/v1/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // Comentário abusivo é soft-deleted e não aparece mais na listagem
        mockMvc.perform(get("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + abusiveCommentId + "')]").doesNotExist());

        // Audit log registrado
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.action == 'REPORT_RESOLVED')]").exists());
    }

    @Test
    @Order(6)
    @DisplayName("Jornada Passo 6: Paywall Freemium - Bloqueio na 2ª comunidade, Upgrade Premium via Webhook e Desbloqueio")
    void step6_paywallAndSubscriptionUpgrade() throws Exception {
        // Usuário A tenta criar 2ª comunidade e é bloqueado pelo Paywall (403)
        CreateCommunityRequest comm2Req = new CreateCommunityRequest(
                "Fase 2 Comunidade de Redes",
                "Segunda comunidade criada pelo usuário Free",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comm2Req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAYWALL_LIMIT_EXCEEDED"));

        // Usuário A inicia checkout de assinatura Premium
        InitiateSubscriptionRequest subReq = new InitiateSubscriptionRequest(
                SubscriptionPlan.PREMIUM,
                "https://app.studywithus.com/account"
        );
        MvcResult subRes = mockMvc.perform(post("/api/v1/subscription")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(subReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plan").value("PREMIUM"))
                .andReturn();
        InitiateSubscriptionResponse subResp = objectMapper.readValue(subRes.getResponse().getContentAsString(), InitiateSubscriptionResponse.class);
        checkoutSessionId = subResp.sessionId();

        // Webhook de pagamento recebido com assinatura válida
        String webhookPayload = String.format("""
                {
                  "id": "evt_full_flow_payment_1",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "%s"
                    }
                  }
                }
                """, checkoutSessionId);

        mockMvc.perform(post("/api/v1/webhooks/subscription")
                        .header("X-Webhook-Signature", "valid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        // Status atualizado para ACTIVE
        mockMvc.perform(get("/api/v1/subscription")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.plan").value("PREMIUM"));

        // Agora Usuário A consegue criar a 2ª comunidade com sucesso!
        mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comm2Req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fase 2 Comunidade de Redes"));
    }

    @Test
    @Order(7)
    @DisplayName("Jornada Passo 7: Observabilidade - Métricas de produto ativas e registradas no Actuator")
    void step7_observabilityMetrics() throws Exception {
        // Health check operacional
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // Métricas de conversão de assinatura
        mockMvc.perform(get("/actuator/metrics/studywithus_subscription_conversions_total")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value").value(greaterThanOrEqualTo(1.0)));

        // Métricas de comentários criados
        mockMvc.perform(get("/actuator/metrics/studywithus_comments_created_total")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value").value(greaterThanOrEqualTo(2.0)));

        // Métricas de badges concedidos
        mockMvc.perform(get("/actuator/metrics/studywithus_badges_awarded_total")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurements[0].value").isNumber());
    }
}
