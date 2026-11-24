package com.studywithus.admin;

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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSystemInfoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String registerUser(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                username, "User " + username, email, "StrongPass123!",
                null, null, null, null, null, "test-client"
        );
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String loginAsAdmin(String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setGlobalRole(GlobalRole.ADMIN);
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
    @DisplayName("Sprint 30 Acceptance: /system/info, /system/health (with graceful DEGRADED on Redis), and /system/overview")
    void systemInfoHealthAndOverview() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String userToken = registerUser("sys_user_" + suffix, "sys_user_" + suffix + "@studywithus.com");
        registerUser("sys_adm_" + suffix, "sys_adm_" + suffix + "@studywithus.com");
        String adminToken = loginAsAdmin("sys_adm_" + suffix);

        // Regular user cannot access /api/v1/admin/system/**
        mockMvc.perform(get("/api/v1/admin/system/health")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // 1. /system/info
        mockMvc.perform(get("/api/v1/admin/system/info")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationName").isNotEmpty())
                .andExpect(jsonPath("$.javaVersion").isNotEmpty())
                .andExpect(jsonPath("$.activeProfiles[0]").value("test"))
                .andExpect(jsonPath("$.uptimeSeconds").isNumber());

        // 2. /system/health (database UP, objectStorage UP, redis DOWN in test -> overallStatus DEGRADED)
        mockMvc.perform(get("/api/v1/admin/system/health")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("DEGRADED"))
                .andExpect(jsonPath("$.database.status").value("UP"))
                .andExpect(jsonPath("$.database.latencyMs").isNumber())
                .andExpect(jsonPath("$.objectStorage.status").value("UP"))
                .andExpect(jsonPath("$.redis.status").value("DOWN"))
                .andExpect(jsonPath("$.outboxAndJobs.status").value("UP"));

        // 3. /system/overview
        mockMvc.perform(get("/api/v1/admin/system/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").isNumber())
                .andExpect(jsonPath("$.activeUsers").isNumber())
                .andExpect(jsonPath("$.activeCommunities").isNumber())
                .andExpect(jsonPath("$.pendingReports").isNumber());
    }
}
