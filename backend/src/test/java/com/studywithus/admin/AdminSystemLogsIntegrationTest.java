package com.studywithus.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.admin.domain.SystemLog;
import com.studywithus.admin.domain.SystemLogRepository;
import com.studywithus.admin.infrastructure.SystemLogIngestionService;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSystemLogsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemLogRepository systemLogRepository;

    private String registerUser(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                username, "User " + username, email, "StrongPass123!",
                null, null, null, null, null, "test-client"
        );
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String loginAsAdmin(String username) throws Exception {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setGlobalRole(GlobalRole.ADMIN);
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(username, "StrongPass123!", "admin-web")
                        )))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    @DisplayName("Sprint 28 Acceptance: filter by level=ERROR, mask sensitive data, cursor paginate, and reconstruct traceId")
    void querySystemLogsAndReconstructTrace() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String userToken = registerUser("loguser_" + suffix, "loguser_" + suffix + "@studywithus.com");
        registerUser("logadm_" + suffix, "logadm_" + suffix + "@studywithus.com");
        String adminToken = loginAsAdmin("logadm_" + suffix);

        // Regular user cannot access /api/v1/admin/system-logs
        mockMvc.perform(get("/api/v1/admin/system-logs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        String traceId = "trace-" + suffix;
        Instant t1 = Instant.now().minusSeconds(10);
        Instant t2 = Instant.now().minusSeconds(5);

        // Record logs via SystemLogIngestionService (verifying masking & persistence)
        SystemLogIngestionService ingestionService = new SystemLogIngestionService(systemLogRepository, objectMapper);
        ingestionService.recordAsync(
                t1, "INFO", "checkin", traceId, "req-1", null,
                "POST", "/api/v1/checkins", 200, 12L,
                "Processing request with Authorization: Bearer secretToken123",
                Map.of("password", "superSecretPass")
        );
        ingestionService.recordAsync(
                t2, "ERROR", "checkin", traceId, "req-1", null,
                "POST", "/api/v1/checkins", 500, 45L,
                "INTERNAL_SERVER_ERROR: simulated failure",
                Map.of("stackTrace", "at com.studywithus.checkin.Service.run(Service.java:42)")
        );

        // 1. Filter by level=ERROR & traceId
        MvcResult listRes = mockMvc.perform(get("/api/v1/admin/system-logs")
                        .param("level", "ERROR")
                        .param("traceId", traceId)
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].level").value("ERROR"))
                .andExpect(jsonPath("$.items[0].traceId").value(traceId))
                .andReturn();

        JsonNode listJson = objectMapper.readTree(listRes.getResponse().getContentAsString());
        assertThat(listJson.get("items").get(0).get("context").asText()).contains("Service.java:42");

        // 2. Reconstruct chronological trace timeline
        MvcResult traceRes = mockMvc.perform(get("/api/v1/admin/system-logs/" + traceId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].level").value("INFO"))
                .andExpect(jsonPath("$[1].level").value("ERROR"))
                .andReturn();

        String body = traceRes.getResponse().getContentAsString();
        assertThat(body).doesNotContain("secretToken123");
        assertThat(body).doesNotContain("superSecretPass");
        assertThat(body).contains("***MASKED***");
    }

    @Test
    @DisplayName("Log storage failure never breaks caller")
    void logPersistenceFailureIsNonBlocking() {
        SystemLogRepository failingRepo = mock(SystemLogRepository.class);
        when(failingRepo.save(any(SystemLog.class))).thenThrow(new RuntimeException("DB down"));
        SystemLogIngestionService service = new SystemLogIngestionService(failingRepo, objectMapper);

        assertThatNoException().isThrownBy(() ->
                service.recordAsync(
                        Instant.now(), "ERROR", "shared", "trace-fail", "req-fail",
                        null, "GET", "/api/v1/test", 500, 10L, "boom", Map.of()
                )
        );
    }
}
