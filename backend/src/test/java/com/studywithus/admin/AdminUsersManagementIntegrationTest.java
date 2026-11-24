package com.studywithus.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.presentation.LoginRequest;
import com.studywithus.identity.presentation.RefreshTokenRequest;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.subscription.domain.Subscription;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionProvider;
import com.studywithus.subscription.domain.SubscriptionRepository;
import com.studywithus.subscription.domain.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUsersManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    private record AuthTokens(UUID userId, String accessToken, String refreshToken) {}

    private AuthTokens registerAndReturnTokens(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                username,
                "User " + username,
                email,
                "StrongPass123!",
                null,
                "USP",
                "Computação",
                "Estudar",
                null,
                "test-client"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthTokens(
                UUID.fromString(json.get("userId").asText()),
                json.get("accessToken").asText(),
                json.get("refreshToken").asText()
        );
    }

    private String loginAndGetAccessToken(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, "StrongPass123!", "admin-web")
                        )))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private void promoteUser(String username, GlobalRole role) {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setGlobalRole(role);
        userRepository.save(user);
    }

    @Test
    @DisplayName("Sprint 27 Acceptance: search by email, view 360, suspend with reason, block login/refresh, reactivate")
    void fullAccountLifecycleAnd360View() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String targetUsername = "target_" + suffix;
        String targetEmail = "target_" + suffix + "@studywithus.com";
        AuthTokens targetTokens = registerAndReturnTokens(targetUsername, targetEmail);

        // Give target user an active PREMIUM subscription
        subscriptionRepository.save(Subscription.builder()
                .id(UUID.randomUUID())
                .userId(targetTokens.userId())
                .provider(SubscriptionProvider.STRIPE)
                .externalId("sub_" + suffix)
                .plan(SubscriptionPlan.PREMIUM)
                .status(SubscriptionStatus.ACTIVE)
                .startedAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

        String adminUsername = "adm27_" + suffix;
        registerAndReturnTokens(adminUsername, "adm27_" + suffix + "@studywithus.com");
        promoteUser(adminUsername, GlobalRole.ADMIN);
        String adminToken = loginAndGetAccessToken(adminUsername);

        // 1. Search by email & filter by plan=PREMIUM
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("search", targetEmail)
                        .param("plan", "PREMIUM")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(targetEmail))
                .andExpect(jsonPath("$.content[0].plan").value("PREMIUM"));

        // 2. View 360 profile
        mockMvc.perform(get("/api/v1/admin/users/" + targetTokens.userId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetTokens.userId().toString()))
                .andExpect(jsonPath("$.email").value(targetEmail))
                .andExpect(jsonPath("$.plan").value("PREMIUM"))
                .andExpect(jsonPath("$.subscriptionStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.communities").isArray())
                .andExpect(jsonPath("$.reports").isArray());

        // 3. Blank reason is rejected with 400
        mockMvc.perform(post("/api/v1/admin/users/" + targetTokens.userId() + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "   "))))
                .andExpect(status().isBadRequest());

        // 4. Suspend with mandatory reason -> revokes refresh tokens immediately
        mockMvc.perform(post("/api/v1/admin/users/" + targetTokens.userId() + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Violação dos termos de uso"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        // 5. Suspended user cannot authenticate or refresh token
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(targetUsername, "StrongPass123!", "test-client")
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_SUSPENDED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(targetTokens.refreshToken(), "test-client")
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_EXPIRED_OR_REVOKED"));

        // 6. Reactivate user
        mockMvc.perform(post("/api/v1/admin/users/" + targetTokens.userId() + "/reactivate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "Apelação aprovada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(targetUsername, "StrongPass123!", "test-client")
                        )))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Changing global_role is restricted to SUPER_ADMIN; revoke-sessions invalidates refresh tokens")
    void roleChangeRestrictedToSuperAdminAndRevokeSessions() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        AuthTokens target = registerAndReturnTokens("t_role_" + suffix, "t_role_" + suffix + "@studywithus.com");

        String adminUsername = "adm_role_" + suffix;
        registerAndReturnTokens(adminUsername, "adm_role_" + suffix + "@studywithus.com");
        promoteUser(adminUsername, GlobalRole.ADMIN);
        String adminToken = loginAndGetAccessToken(adminUsername);

        String superUsername = "super_" + suffix;
        registerAndReturnTokens(superUsername, "super_" + suffix + "@studywithus.com");
        promoteUser(superUsername, GlobalRole.SUPER_ADMIN);
        String superToken = loginAndGetAccessToken(superUsername);

        // ADMIN cannot change global_role -> 403
        mockMvc.perform(patch("/api/v1/admin/users/" + target.userId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "globalRole", "SUPPORT",
                                "reason", "Tentativa sem permissão"
                        ))))
                .andExpect(status().isForbidden());

        // SUPER_ADMIN can change global_role -> 200
        mockMvc.perform(patch("/api/v1/admin/users/" + target.userId() + "/role")
                        .header("Authorization", "Bearer " + superToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "globalRole", "SUPPORT",
                                "reason", "Promoção para equipe de suporte"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalRole").value("SUPPORT"));

        // Target's existing refresh token was revoked on role change
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(target.refreshToken(), "test-client")
                        )))
                .andExpect(status().isUnauthorized());
    }
}
