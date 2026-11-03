package com.studywithus.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.notification.domain.NotificationType;
import com.studywithus.notification.domain.RealtimeNotification;
import com.studywithus.notification.infrastructure.SseRealtimeNotificationAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RealtimeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SseRealtimeNotificationAdapter sseAdapter;

    @Test
    @DisplayName("Sprint 11 Teste: Usuário A cria atividade -> Usuário B recebe atualização via realtime SSE")
    void userACreatesActivity_userBReceivesRealtimeUpdate() throws Exception {
        // 1. Register User A
        String tokenA = registerUser("realtime_a", "Alice Realtime", "alice_rt@uni.edu");
        UUID userAId = getUserId(tokenA);

        // 2. Register User B
        String tokenB = registerUser("realtime_b", "Bob Realtime", "bob_rt@uni.edu");
        UUID userBId = getUserId(tokenB);

        // 3. User A creates public community
        CreateCommunityRequest createComm = new CreateCommunityRequest(
                "Realtime Community",
                "Realtime event testing group",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );
        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createComm)))
                .andExpect(status().isCreated())
                .andReturn();
        CommunityResponse comm = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class);
        UUID communityId = comm.id();

        // 4. User B joins the community
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isCreated());

        // 5. User B connects to SSE stream via endpoint with token query param
        mockMvc.perform(get("/api/v1/realtime/stream?token=" + tokenB)
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());

        // Register User B on sseAdapter to capture notifications
        sseAdapter.subscribe(userBId);
        assertThat(sseAdapter.getConnectedUserCount()).isGreaterThanOrEqualTo(1);

        // 6. User A publishes a check-in in the community
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Meu primeiro check-in em tempo real!",
                List.of("study_photo.jpg")
        );

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated());

        // 7. Test direct delivery of notifications
        RealtimeNotification testNotification = RealtimeNotification.create(
                NotificationType.NEW_CHECKIN,
                null,
                communityId,
                "Nova atividade na comunidade",
                "Um participante publicou um novo check-in de estudos.",
                java.util.Map.of("communityId", communityId.toString(), "authorUserId", userAId.toString())
        );

        sseAdapter.broadcastToCommunity(communityId, testNotification, userAId);
        sseAdapter.sendToUser(userBId, testNotification);
    }

    private String registerUser(String username, String name, String email) throws Exception {
        RegisterRequest req = new RegisterRequest(
                username,
                name,
                email,
                "password123",
                LocalDate.of(2000, 1, 1),
                "USP",
                "Ciência da Computação",
                "Prêmio",
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
}
