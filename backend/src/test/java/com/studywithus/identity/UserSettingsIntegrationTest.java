package com.studywithus.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.domain.User;
import com.studywithus.identity.domain.UserRepository;
import com.studywithus.identity.domain.UserStatus;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.ChangePasswordRequest;
import com.studywithus.identity.presentation.LoginRequest;
import com.studywithus.identity.presentation.RefreshTokenRequest;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.identity.presentation.UpdatePrivacyRequest;
import com.studywithus.notification.presentation.UpdateNotificationPreferencesRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSettingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private AuthResponse registerUser(String prefix) throws Exception {
        String unique = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                unique,
                                "User " + prefix,
                                unique + "@uni.edu",
                                "password123",
                                LocalDate.of(2000, 1, 1),
                                "USP",
                                "Computer Science",
                                "Master backend",
                                "Bio for " + prefix,
                                "dev-" + unique
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
    }

    @Test
    @DisplayName("Change password: success, wrong current password, mismatch confirmation, login with new password")
    void shouldChangePasswordAndAllowLoginWithNewPassword() throws Exception {
        AuthResponse auth = registerUser("chgpw");

        // Mismatch confirmation -> 400
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "password123", "newpassword123", "different123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));

        // Wrong current password -> 400
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "wrongpass", "newpassword123", "newpassword123"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        // Valid password change -> 204
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "password123", "newpassword123", "newpassword123"
                        ))))
                .andExpect(status().isNoContent());

        // Old password login fails
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                auth.username(), "password123", "dev-1"
                        ))))
                .andExpect(status().isUnauthorized());

        // New password login succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                auth.username(), "newpassword123", "dev-1"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("Privacy toggle: public -> private hides fields from others and enforces 10-minute rate limit")
    void shouldTogglePrivacyAndEnforceRateLimit() throws Exception {
        AuthResponse owner = registerUser("privowner");
        AuthResponse viewer = registerUser("privviewer");

        // Owner sets profile to private
        mockMvc.perform(patch("/api/v1/users/me/privacy")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePrivacyRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrivate").value(true))
                .andExpect(jsonPath("$.privacyUpdatedAt").isNotEmpty());

        // Self still sees full profile
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrivate").value(true))
                .andExpect(jsonPath("$.institution").value("USP"))
                .andExpect(jsonPath("$.email").value(owner.email()));

        // Another user sees only public identity fields (id, username, name, avatarUrl, bio, isPrivate, status)
        mockMvc.perform(get("/api/v1/users/" + owner.userId())
                        .header("Authorization", "Bearer " + viewer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(owner.userId().toString()))
                .andExpect(jsonPath("$.username").value(owner.username()))
                .andExpect(jsonPath("$.name").value(owner.name()))
                .andExpect(jsonPath("$.bio").value("Bio for privowner"))
                .andExpect(jsonPath("$.isPrivate").value(true))
                .andExpect(jsonPath("$.institution").isEmpty())
                .andExpect(jsonPath("$.course").isEmpty())
                .andExpect(jsonPath("$.studyGoal").isEmpty())
                .andExpect(jsonPath("$.email").isEmpty())
                .andExpect(jsonPath("$.currentPoints").value(0))
                .andExpect(jsonPath("$.checkinsCount").value(0));

        // Second change within 10 minutes -> 429 Too Many Requests
        mockMvc.perform(patch("/api/v1/users/me/privacy")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePrivacyRequest(false))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("PRIVACY_RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("Deactivate account: archives user, revokes refresh token, hides profile, reactivates on valid login")
    void shouldDeactivateAndReactivateOnValidLogin() throws Exception {
        AuthResponse userAuth = registerUser("deact");
        AuthResponse viewer = registerUser("deactviewer");

        // Deactivate account
        mockMvc.perform(post("/api/v1/users/me/deactivate")
                        .header("Authorization", "Bearer " + userAuth.accessToken()))
                .andExpect(status().isNoContent());

        // Refresh token is revoked
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(
                                userAuth.refreshToken(), "dev-refresh"
                        ))))
                .andExpect(status().isUnauthorized());

        // Profile hidden from other users
        mockMvc.perform(get("/api/v1/users/" + userAuth.userId())
                        .header("Authorization", "Bearer " + viewer.accessToken()))
                .andExpect(status().isNotFound());

        // Wrong password does NOT reactivate
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                userAuth.username(), "wrongpassword", "dev-login"
                        ))))
                .andExpect(status().isUnauthorized());

        User stillDeactivated = userRepository.findById(userAuth.userId()).orElseThrow();
        assertThat(stillDeactivated.getStatus()).isEqualTo(UserStatus.DEACTIVATED);

        // Valid password reactivates account
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                userAuth.username(), "password123", "dev-login"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        User reactivated = userRepository.findById(userAuth.userId()).orElseThrow();
        assertThat(reactivated.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(reactivated.getDeactivatedAt()).isNull();

        // Profile is visible again
        mockMvc.perform(get("/api/v1/users/" + userAuth.userId())
                        .header("Authorization", "Bearer " + viewer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userAuth.username()));

        // Admin-suspended account cannot self-reactivate
        reactivated.setStatus(UserStatus.SUSPENDED);
        userRepository.save(reactivated);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                userAuth.username(), "password123", "dev-login"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_SUSPENDED"));
    }

    @Test
    @DisplayName("Delete account permanently: deletes user, checkins, and ranking entries")
    void shouldDeleteAccountPermanently() throws Exception {
        AuthResponse auth = registerUser("deluser");
        AuthResponse viewer = registerUser("delviewer");

        // Create community and checkin to populate checkins & ranking_entries
        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                "Delete Test Community",
                                "Desc",
                                null,
                                CommunityVisibility.PUBLIC,
                                "America/Sao_Paulo"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID communityId = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class).id();

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId, "Checkin before delete", List.of()
                        ))))
                .andExpect(status().isCreated());

        // Delete account permanently
        mockMvc.perform(delete("/api/v1/users/me")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isNoContent());

        // User is gone
        assertThat(userRepository.findById(auth.userId())).isEmpty();

        // Subsequent login fails with 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                auth.username(), "password123", "dev-login"
                        ))))
                .andExpect(status().isUnauthorized());

        // Profile returns 404
        mockMvc.perform(get("/api/v1/users/" + auth.userId())
                        .header("Authorization", "Bearer " + viewer.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Notification preferences GET and PUT")
    void shouldGetAndUpdateNotificationPreferences() throws Exception {
        AuthResponse auth = registerUser("notifpref");

        mockMvc.perform(get("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentsEnabled").value(true))
                .andExpect(jsonPath("$.rankingEnabled").value(true));

        mockMvc.perform(put("/api/v1/notifications/preferences")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateNotificationPreferencesRequest(
                                false, true, false, true, false, true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentsEnabled").value(false))
                .andExpect(jsonPath("$.rankingEnabled").value(true))
                .andExpect(jsonPath("$.challengeEnabled").value(false))
                .andExpect(jsonPath("$.invitationEnabled").value(true))
                .andExpect(jsonPath("$.reminderEnabled").value(false))
                .andExpect(jsonPath("$.streakEnabled").value(true));
    }
}
