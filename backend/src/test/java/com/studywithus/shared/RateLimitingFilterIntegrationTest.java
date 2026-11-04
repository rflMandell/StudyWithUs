package com.studywithus.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.identity.presentation.LoginRequest;
import com.studywithus.shared.infrastructure.RateLimitingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.rate-limiting.enabled=true")
class RateLimitingFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitingService rateLimitingService;

    @Test
    @DisplayName("Sprint 12: Exceeding rate limit on /api/v1/auth/login returns 429 Too Many Requests")
    void loginRateLimiting_returns429WhenExceeded() throws Exception {
        LoginRequest req = new LoginRequest("test_user", "wrong_password", "device-test");
        String content = objectMapper.writeValueAsString(req);

        // Pre-fill counter up to the max (20) for test IP "198.51.100.1"
        String testIp = "198.51.100.1";
        String key = "POST:/api/v1/auth/login:" + testIp;
        for (int i = 0; i < 20; i++) {
            rateLimitingService.tryAcquire(key, 20, 60);
        }

        // The 21st request from this IP must be blocked with 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", testIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));

        rateLimitingService.reset(key);
    }
}
