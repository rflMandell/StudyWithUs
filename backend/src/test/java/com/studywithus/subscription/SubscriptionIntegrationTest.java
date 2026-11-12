package com.studywithus.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.subscription.domain.SubscriptionPlan;
import com.studywithus.subscription.domain.SubscriptionStatus;
import com.studywithus.subscription.presentation.InitiateSubscriptionRequest;
import com.studywithus.subscription.presentation.InitiateSubscriptionResponse;
import com.studywithus.subscription.presentation.SubscriptionResponse;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SubscriptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String userToken;
    private static UUID userId;
    private static String checkoutSessionId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register user on Free plan")
    void setup() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "user_monetization_s18",
                "Freemium User",
                "freemium_s18@studywithus.com",
                "password123",
                LocalDate.of(2000, 4, 12),
                "USP",
                "Economia",
                "Estudar monetização",
                "Bio Monetization",
                "monetize-s18"
        );
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
        userToken = auth.accessToken();
        userId = auth.userId();

        // Check initial subscription is FREE
        mockMvc.perform(get("/api/v1/subscription")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @Order(2)
    @DisplayName("Step 1: Free user creates 1st community (within free limit, success)")
    void step1_freeUserCreatesFirstCommunity() throws Exception {
        CreateCommunityRequest comm1 = new CreateCommunityRequest(
                "Free First Community",
                "Allowed community for free tier",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comm1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Free First Community"));
    }

    @Test
    @Order(3)
    @DisplayName("Step 2: Free user tries to create 2nd community and is BLOCKED by paywall")
    void step2_freeUserBlockedBeyondLimit() throws Exception {
        CreateCommunityRequest comm2 = new CreateCommunityRequest(
                "Free Second Community",
                "Should be blocked by paywall",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comm2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PAYWALL_LIMIT_EXCEEDED"));
    }

    @Test
    @Order(4)
    @DisplayName("Step 3: User initiates Premium subscription checkout via POST /api/v1/subscription")
    void step3_userInitiatesPremiumSubscription() throws Exception {
        InitiateSubscriptionRequest req = new InitiateSubscriptionRequest(
                SubscriptionPlan.PREMIUM,
                "https://app.studywithus.com/dashboard"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/subscription")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscriptionId").isNotEmpty())
                .andExpect(jsonPath("$.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.checkoutUrl").isNotEmpty())
                .andExpect(jsonPath("$.plan").value("PREMIUM"))
                .andReturn();

        InitiateSubscriptionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                InitiateSubscriptionResponse.class
        );
        checkoutSessionId = response.sessionId();
        assertThat(checkoutSessionId).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("Step 4: Webhook without valid signature is rejected (401 Unauthorized)")
    void step4_webhookRejectsInvalidSignature() throws Exception {
        String payload = """
                {
                  "id": "evt_tampered",
                  "type": "checkout.session.completed"
                }
                """;

        mockMvc.perform(post("/api/v1/webhooks/subscription")
                        .header("X-Webhook-Signature", "invalid-fake-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    @DisplayName("Step 5: Webhook of approved payment received and validated with idempotency")
    void step5_webhookPaymentApproved() throws Exception {
        String payload = String.format("""
                {
                  "id": "evt_stripe_payment_999",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "%s"
                    }
                  }
                }
                """, checkoutSessionId);

        mockMvc.perform(post("/api/v1/webhooks/subscription")
                        .header("X-Webhook-Signature", "test-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));

        // Idempotency: Send same webhook again, must return 200 OK without errors
        mockMvc.perform(post("/api/v1/webhooks/subscription")
                        .header("X-Webhook-Signature", "test-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }

    @Test
    @Order(7)
    @DisplayName("Step 6: Status of subscription updated to ACTIVE and PREMIUM")
    void step6_subscriptionStatusIsActive() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/subscription")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PREMIUM"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        SubscriptionResponse sub = objectMapper.readValue(result.getResponse().getContentAsString(), SubscriptionResponse.class);
        assertThat(sub.expiresAt()).isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("Step 7: Premium user can now create 2nd community with success!")
    void step7_premiumUserCreatesSecondCommunity() throws Exception {
        CreateCommunityRequest comm2 = new CreateCommunityRequest(
                "Premium Second Community",
                "Created successfully after upgrading to Premium!",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );

        mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(comm2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Premium Second Community"));
    }

    @Test
    @Order(9)
    @DisplayName("Step 8: User cancels subscription via POST /api/v1/subscription/cancel")
    void step8_cancelSubscription() throws Exception {
        mockMvc.perform(post("/api/v1/subscription/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }
}
