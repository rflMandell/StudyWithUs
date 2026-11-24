package com.studywithus.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.admin.application.AdminAuditService;
import com.studywithus.admin.domain.SystemLog;
import com.studywithus.admin.domain.SystemLogRepository;
import com.studywithus.admin.infrastructure.SystemLogIngestionService;
import com.studywithus.admin.infrastructure.SystemLogRetentionJob;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.presentation.LoginRequest;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.moderation.domain.AuditLog;
import com.studywithus.moderation.domain.AuditLogRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityHardeningIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemLogRepository systemLogRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AdminAuditService adminAuditService;

    @Autowired
    private SystemLogRetentionJob systemLogRetentionJob;

    @Autowired
    private EntityManager entityManager;

    private record TestUser(UUID id, String username, String token) {}

    private TestUser registerUser(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                username, "User " + username, email, "StrongPass123!",
                null, null, null, null, null, "test-client"
        );
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        var json = objectMapper.readTree(res.getResponse().getContentAsString());
        return new TestUser(
                UUID.fromString(json.get("userId").asText()),
                username,
                json.get("accessToken").asText()
        );
    }

    private TestUser promoteAndLogin(TestUser user, GlobalRole role) throws Exception {
        User domainUser = userRepository.findByUsername(user.username()).orElseThrow();
        domainUser.setGlobalRole(role);
        userRepository.save(domainUser);

        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(user.username(), "StrongPass123!", "admin-web")
                        )))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
        return new TestUser(user.id(), user.username(), token);
    }

    @Test
    @DisplayName("Negative RBAC Matrix: anonymous, USER, community OWNER, SUPPORT, ADMIN and SUPER_ADMIN")
    void negativeAuthorizationMatrixCoversAllAdminEndpoints() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        TestUser regularUser = registerUser("rbac_u_" + suffix, "rbac_u_" + suffix + "@studywithus.com");
        TestUser communityOwner = registerUser("rbac_own_" + suffix, "rbac_own_" + suffix + "@studywithus.com");
        TestUser supportUser = promoteAndLogin(registerUser("rbac_sup_" + suffix, "rbac_sup_" + suffix + "@studywithus.com"), GlobalRole.SUPPORT);
        TestUser adminUser = promoteAndLogin(registerUser("rbac_adm_" + suffix, "rbac_adm_" + suffix + "@studywithus.com"), GlobalRole.ADMIN);
        TestUser superAdmin = promoteAndLogin(registerUser("rbac_sa_" + suffix, "rbac_sa_" + suffix + "@studywithus.com"), GlobalRole.SUPER_ADMIN);

        // Create a community so communityOwner has contextual OWNER role (still global_role=USER)
        mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + communityOwner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Community " + suffix,
                                "description", "Testing isolation",
                                "visibility", "PUBLIC",
                                "rankingType", "WEEKLY",
                                "timezone", "UTC"
                        ))))
                .andExpect(status().isCreated());

        List<String> getEndpoints = List.of(
                "/api/v1/admin/users",
                "/api/v1/admin/users/" + regularUser.id(),
                "/api/v1/admin/security/session",
                "/api/v1/admin/system-logs",
                "/api/v1/admin/system-logs/nonexistent-trace",
                "/api/v1/admin/audit-logs",
                "/api/v1/admin/audit-logs/export",
                "/api/v1/admin/system/info",
                "/api/v1/admin/system/health",
                "/api/v1/admin/system/overview"
        );

        // 1. Anonymous -> 401 on all GET endpoints
        for (String endpoint : getEndpoints) {
            mockMvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        }

        // 2. Regular USER and Community OWNER -> 403 on all GET endpoints
        for (String token : List.of(regularUser.token(), communityOwner.token())) {
            for (String endpoint : getEndpoints) {
                mockMvc.perform(get(endpoint).header("Authorization", "Bearer " + token))
                        .andExpect(status().isForbidden());
            }
            mockMvc.perform(post("/api/v1/admin/users/" + regularUser.id() + "/suspend")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("reason", "denied"))))
                    .andExpect(status().isForbidden());
        }

        // 3. SUPPORT -> 403 on ADMIN/SUPER_ADMIN restricted endpoints
        mockMvc.perform(post("/api/v1/admin/users/" + regularUser.id() + "/suspend")
                        .header("Authorization", "Bearer " + supportUser.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "denied"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/security/2fa/setup")
                        .header("Authorization", "Bearer " + supportUser.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/audit-logs/export")
                        .header("Authorization", "Bearer " + supportUser.token()))
                .andExpect(status().isForbidden());

        // 4. ADMIN -> 403 on SUPER_ADMIN-only role change
        mockMvc.perform(patch("/api/v1/admin/users/" + regularUser.id() + "/role")
                        .header("Authorization", "Bearer " + adminUser.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("globalRole", "ADMIN", "reason", "denied"))))
                .andExpect(status().isForbidden());

        // 5. SUPER_ADMIN -> 200 on role change
        mockMvc.perform(patch("/api/v1/admin/users/" + regularUser.id() + "/role")
                        .header("Authorization", "Bearer " + superAdmin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("globalRole", "SUPPORT", "reason", "approved"))))
                .andExpect(status().isOk());
    }

    @Test
    @Transactional
    @DisplayName("PII & Secret Masking Audit: system_logs and audit_logs redact sensitive values")
    void maskingAuditInSystemAndAuditLogs() {
        String traceId = "mask-audit-" + UUID.randomUUID().toString().substring(0, 6);
        SystemLogIngestionService ingestion = new SystemLogIngestionService(systemLogRepository, objectMapper);
        ingestion.recordAsync(
                Instant.now(), "WARN", "identity", traceId, "req-mask",
                null, "POST", "/api/v1/auth/login", 401, 5L,
                "Failed with Bearer topSecretToken123 and password=myRawPassword",
                Map.of("refreshToken", "raw-refresh-999", "totpSecret", "JBSWY3DPEHPK3PXP")
        );

        List<SystemLog> sysLogs = systemLogRepository.findByTraceIdOrderByTimestampAsc(traceId);
        assertThat(sysLogs).hasSize(1);
        assertThat(sysLogs.get(0).getMessage()).doesNotContain("topSecretToken123", "myRawPassword");
        assertThat(sysLogs.get(0).getContext()).doesNotContain("raw-refresh-999", "JBSWY3DPEHPK3PXP");

        UUID actorId = UUID.randomUUID();
        AuditLog savedAudit = adminAuditService.record(
                actorId,
                "MASK_AUDIT_TEST",
                "USER",
                UUID.randomUUID(),
                "reason with Bearer leakTokenABC",
                Map.of("passwordHash", "secretHash123"),
                Map.of("totpSecret", "SECRETBASE32")
        );
        assertThat(savedAudit.getReason()).doesNotContain("leakTokenABC");
        assertThat(savedAudit.getBeforeState()).doesNotContain("secretHash123");
        assertThat(savedAudit.getAfterState()).doesNotContain("SECRETBASE32");
    }

    @Test
    @DisplayName("Index, EXPLAIN query plan validation, and system_logs retention purge")
    void indexMigrationsExplainPlansAndRetentionPolicy() throws Exception {
        // 1. Verify migrations V8, V9, V10 define expected performance indexes
        String v8 = new String(new ClassPathResource("db/migration/V8__admin_global_role_and_2fa.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String v9 = new String(new ClassPathResource("db/migration/V9__system_logs.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String v10 = new String(new ClassPathResource("db/migration/V10__admin_audit_logs_evolution.sql").getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(v8).contains("idx_users_global_role");
        assertThat(v9).contains("idx_system_logs_timestamp_id", "idx_system_logs_trace_id", "idx_system_logs_level_timestamp");
        assertThat(v10).contains("idx_audit_logs_actor_created", "idx_audit_logs_action_created", "idx_audit_logs_target_created");

        // 2. Run EXPLAIN on paginated queries
        List<?> usersPlan = entityManager.createNativeQuery("EXPLAIN SELECT * FROM users ORDER BY created_at DESC LIMIT 20").getResultList();
        List<?> sysLogsPlan = entityManager.createNativeQuery("EXPLAIN SELECT * FROM system_logs ORDER BY timestamp DESC, id DESC LIMIT 30").getResultList();
        List<?> auditPlan = entityManager.createNativeQuery("EXPLAIN SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 20").getResultList();
        assertThat(usersPlan).isNotEmpty();
        assertThat(sysLogsPlan).isNotEmpty();
        assertThat(auditPlan).isNotEmpty();

        // 3. Retention job purges logs older than 30 days while keeping fresh logs
        String oldTrace = "old-trace-" + UUID.randomUUID().toString().substring(0, 6);
        String newTrace = "new-trace-" + UUID.randomUUID().toString().substring(0, 6);
        systemLogRepository.save(SystemLog.builder()
                .id(UUID.randomUUID())
                .timestamp(Instant.now().minus(45, ChronoUnit.DAYS))
                .level("INFO").module("shared").traceId(oldTrace).message("expired")
                .build());
        systemLogRepository.save(SystemLog.builder()
                .id(UUID.randomUUID())
                .timestamp(Instant.now())
                .level("INFO").module("shared").traceId(newTrace).message("fresh")
                .build());

        int purged = systemLogRetentionJob.purgeExpiredLogs();
        assertThat(purged).isGreaterThanOrEqualTo(1);
        assertThat(systemLogRepository.findByTraceIdOrderByTimestampAsc(oldTrace)).isEmpty();
        assertThat(systemLogRepository.findByTraceIdOrderByTimestampAsc(newTrace)).hasSize(1);
    }
}
