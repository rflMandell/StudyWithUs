package com.studywithus.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.presentation.LoginRequest;
import com.studywithus.identity.presentation.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuditLogsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private UUID registerUser(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                username, "User " + username, email, "StrongPass123!",
                null, null, null, null, null, "test-client"
        );
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).get("userId").asText());
    }

    private String loginWithRole(String username, GlobalRole role) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setGlobalRole(role);
        userRepository.save(user);

        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, "StrongPass123!", "admin-web")
                        )))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("Sprint 29 Acceptance: mutative admin action persists immutable audit log with actor, IP, reason and before/after state")
    void mutativeAdminActionPersistsDetailedAuditLogAndExports() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID targetId = registerUser("aud_target_" + suffix, "aud_target_" + suffix + "@studywithus.com");
        UUID superId = registerUser("aud_super_" + suffix, "aud_super_" + suffix + "@studywithus.com");
        String superToken = loginWithRole("aud_super_" + suffix, GlobalRole.SUPER_ADMIN);

        // Execute mutative action (change role)
        mockMvc.perform(patch("/api/v1/admin/users/" + targetId + "/role")
                        .header("Authorization", "Bearer " + superToken)
                        .header("X-Forwarded-For", "203.0.113.42")
                        .header("User-Agent", "AdminBackofficeTest/1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "globalRole", "SUPPORT",
                                "reason", "Onboarding de suporte"
                        ))))
                .andExpect(status().isOk());

        // Also execute a mutative 2FA setup endpoint covered by AdminMutationAuditInterceptor
        mockMvc.perform(post("/api/v1/admin/security/2fa/setup")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk());

        // Query GET /api/v1/admin/audit-logs filtering by actorId
        MvcResult auditRes = mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("actorId", superId.toString())
                        .param("action", "USER_GLOBAL_ROLE_CHANGED")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actorId").value(superId.toString()))
                .andExpect(jsonPath("$.content[0].actorRole").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.content[0].action").value("USER_GLOBAL_ROLE_CHANGED"))
                .andExpect(jsonPath("$.content[0].targetType").value("USER"))
                .andExpect(jsonPath("$.content[0].targetId").value(targetId.toString()))
                .andExpect(jsonPath("$.content[0].reason").value("Onboarding de suporte"))
                .andExpect(jsonPath("$.content[0].ipAddress").value("203.0.113.42"))
                .andExpect(jsonPath("$.content[0].userAgent").value("AdminBackofficeTest/1.0"))
                .andExpect(jsonPath("$.content[0].requestId").isNotEmpty())
                .andReturn();

        JsonNode entry = objectMapper.readTree(auditRes.getResponse().getContentAsString()).get("content").get(0);
        assertThat(entry.get("beforeState").asText()).contains("USER");
        assertThat(entry.get("afterState").asText()).contains("SUPPORT");

        // Verify interceptor also recorded POST /api/v1/admin/security/2fa/setup
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("actorId", superId.toString())
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Export CSV & JSON
        MvcResult csvRes = mockMvc.perform(get("/api/v1/admin/audit-logs/export")
                        .param("actorId", superId.toString())
                        .param("format", "csv")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"audit-logs.csv\""))
                .andReturn();
        assertThat(csvRes.getResponse().getContentAsString()).contains("USER_GLOBAL_ROLE_CHANGED");

        mockMvc.perform(get("/api/v1/admin/audit-logs/export")
                        .param("actorId", superId.toString())
                        .param("format", "json")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"audit-logs.json\""))
                .andExpect(jsonPath("$.length()").value(2));

        // Immutability check: DELETE is not allowed on audit-logs
        mockMvc.perform(delete("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + superToken))
                .andExpect(status().isMethodNotAllowed());
    }
}
