package com.studywithus.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.identity.domain.GlobalRole;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.identity.infrastructure.JwtService;
import com.studywithus.identity.infrastructure.TotpService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRbacAndSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TotpService totpService;

    private String registerUser(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(
                username,
                "Test " + username,
                email,
                "StrongPass123!",
                null,
                null,
                null,
                null,
                null,
                "test-client"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    private void promoteUser(String username, GlobalRole role) {
        User user = userRepository.findByUsername(username).orElseThrow();
        user.setGlobalRole(role);
        userRepository.save(user);
    }

    @Test
    @DisplayName("Anonymous user receives 401 Unauthorized on /api/v1/admin/users")
    void anonymousUserIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Sprint 26 Acceptance: USER receives 403 Forbidden; ADMIN receives 200 OK")
    void regularUserGets403AndAdminGets200() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String userToken = registerUser("regular_" + suffix, "regular_" + suffix + "@studywithus.com");

        // Regular user (global_role = USER) -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Register and promote another user to ADMIN
        String adminUsername = "admin_" + suffix;
        registerUser(adminUsername, "admin_" + suffix + "@studywithus.com");
        promoteUser(adminUsername, GlobalRole.ADMIN);

        // Login as ADMIN -> verify shorter TTL and globalRole claim
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(adminUsername, "StrongPass123!", "admin-web")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalRole").value("ADMIN"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String adminToken = loginJson.get("accessToken").asText();
        assertThat(jwtService.extractGlobalRole(adminToken)).isEqualTo(GlobalRole.ADMIN);

        // Admin accesses GET /api/v1/admin/users -> 200 OK
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("SUPPORT can list users but cannot manage 2FA (restricted to ADMIN and SUPER_ADMIN)")
    void supportRoleCannotSetup2fa() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String supportUsername = "support_" + suffix;
        registerUser(supportUsername, "support_" + suffix + "@studywithus.com");
        promoteUser(supportUsername, GlobalRole.SUPPORT);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(supportUsername, "StrongPass123!", "support-web")
                        )))
                .andExpect(status().isOk())
                .andReturn();

        String supportToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/security/2fa/setup")
                        .header("Authorization", "Bearer " + supportToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Suspended admin is immediately blocked on subsequent requests")
    void suspendedAdminIsBlockedImmediately() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminUsername = "susp_admin_" + suffix;
        registerUser(adminUsername, "susp_admin_" + suffix + "@studywithus.com");
        promoteUser(adminUsername, GlobalRole.ADMIN);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(adminUsername, "StrongPass123!", "admin-web")
                        )))
                .andExpect(status().isOk())
                .andReturn();

        String adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Suspend admin in DB while token is still unexpired
        User admin = userRepository.findByUsername(adminUsername).orElseThrow();
        admin.setStatus(UserStatus.SUSPENDED);
        userRepository.save(admin);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin 2FA/TOTP setup, verification, and enforced login flow")
    void adminTwoFactorLifecycle() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String adminUsername = "totp_admin_" + suffix;
        registerUser(adminUsername, "totp_admin_" + suffix + "@studywithus.com");
        promoteUser(adminUsername, GlobalRole.ADMIN);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(adminUsername, "StrongPass123!", "admin-web")
                        )))
                .andExpect(status().isOk())
                .andReturn();

        String initialToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();

        // 1. Setup 2FA
        MvcResult setupResult = mockMvc.perform(post("/api/v1/admin/security/2fa/setup")
                        .header("Authorization", "Bearer " + initialToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secret").isNotEmpty())
                .andExpect(jsonPath("$.otpAuthUri").isNotEmpty())
                .andReturn();

        String secret = objectMapper.readTree(setupResult.getResponse().getContentAsString())
                .get("secret").asText();
        String validCode = totpService.generateCurrentCode(secret);

        // 2. Verify and enable 2FA
        mockMvc.perform(post("/api/v1/admin/security/2fa/verify")
                        .header("Authorization", "Bearer " + initialToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", validCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totpEnabled").value(true));

        // 3. Old token without twoFactorVerified claim is now rejected on /api/v1/admin/**
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + initialToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TOTP_VERIFICATION_REQUIRED"));

        // 4. Login without totpCode -> 401 TOTP_REQUIRED
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(adminUsername, "StrongPass123!", "admin-web")
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOTP_REQUIRED"));

        // 5. Login with invalid totpCode -> 401 INVALID_TOTP_CODE
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(adminUsername, "StrongPass123!", "admin-web", "000000")
                        )))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"));

        // 6. Login with valid totpCode -> 200 OK and access to /api/v1/admin/users
        MvcResult verifiedLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(adminUsername, "StrongPass123!", "admin-web", totpService.generateCurrentCode(secret))
                        )))
                .andExpect(status().isOk())
                .andReturn();

        String verifiedToken = objectMapper.readTree(verifiedLogin.getResponse().getContentAsString())
                .get("accessToken").asText();
        assertThat(jwtService.isTwoFactorVerified(verifiedToken)).isTrue();

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + verifiedToken))
                .andExpect(status().isOk());
    }
}
