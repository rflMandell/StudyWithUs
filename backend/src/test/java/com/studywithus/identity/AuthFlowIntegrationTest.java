package com.studywithus.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.LoginRequest;
import com.studywithus.identity.presentation.RefreshTokenRequest;
import com.studywithus.identity.presentation.RegisterRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String accessToken;
    private static String refreshToken;

    @Test
    @Order(1)
    @DisplayName("1. Register: Should create a new user and return tokens")
    void step1_register() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "mariacoder",
                "Maria Silva",
                "maria@university.br",
                "strongSecret123",
                LocalDate.of(2001, 5, 20),
                "USP",
                "Ciência da Computação",
                "Passar em cálculo e IA",
                "Estudando firme todos os dias",
                "web-chrome-client"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("mariacoder"))
                .andExpect(jsonPath("$.email").value("maria@university.br"))
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = response.accessToken();
        refreshToken = response.refreshToken();

        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("2. Login: Should authenticate user credentials and return new token pair")
    void step2_login() throws Exception {
        LoginRequest request = new LoginRequest(
                "mariacoder",
                "strongSecret123",
                "web-chrome-client"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value("mariacoder"))
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = response.accessToken();
        refreshToken = response.refreshToken();
    }

    @Test
    @Order(3)
    @DisplayName("3. Protected Resource: Should access /users/me using Bearer token")
    void step3_accessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mariacoder"))
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.institution").value("USP"));
    }

    @Test
    @Order(4)
    @DisplayName("4. Security: Should reject access to /users/me without Bearer token (401)")
    void step4_unauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @Order(5)
    @DisplayName("5. Refresh Token: Should rotate tokens and provide a new valid pair")
    void step5_refreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken, "web-chrome-client");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        String newAccessToken = response.accessToken();
        String newRefreshToken = response.refreshToken();

        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // Update tokens for subsequent steps
        accessToken = newAccessToken;
        refreshToken = newRefreshToken;

        // Verify that the new access token can access protected endpoints
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mariacoder"));
    }

    @Test
    @Order(6)
    @DisplayName("6. Logout: Should invalidate the refresh token")
    void step6_logout() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest(refreshToken, "web-chrome-client");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Attempting to refresh with the logged-out refresh token must be rejected (401)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_EXPIRED_OR_REVOKED"));
    }
}
