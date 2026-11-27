package com.studywithus.social;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.identity.presentation.UpdatePrivacyRequest;
import com.studywithus.identity.presentation.UserProfileResponse;
import com.studywithus.identity.presentation.UserSearchItemResponse;
import com.studywithus.social.application.FollowRelationshipStatus;
import com.studywithus.social.presentation.FeedItemResponse;
import com.studywithus.social.presentation.FeedResponse;
import com.studywithus.social.presentation.FollowActionResponse;
import com.studywithus.social.presentation.FollowStatusResponse;
import com.studywithus.social.presentation.PendingFollowRequestResponse;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserSearchPrivacyFollowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String aliceToken;
    private static UUID aliceId;
    private static String bobToken;
    private static UUID bobId;
    private static String charlieToken;
    private static UUID charlieId;
    private static String deactivatedToken;
    private static UUID deactivatedId;
    private static UUID publicCommunityId;
    private static UUID privateCommunityId;
    private static UUID pendingRequestId;

    @Test
    @Order(1)
    @DisplayName("Setup: register users and create communities and check-ins")
    void setup() throws Exception {
        AuthResponse alice = register("spf_alice", "Alice Searchable", "spf_alice@uni.edu", "dev-spf-alice");
        aliceToken = alice.accessToken();
        aliceId = alice.userId();

        AuthResponse bob = register("spf_bob", "Bob Private", "spf_bob@uni.edu", "dev-spf-bob");
        bobToken = bob.accessToken();
        bobId = bob.userId();

        AuthResponse charlie = register("spf_charlie", "Charlie Public", "spf_charlie@uni.edu", "dev-spf-charlie");
        charlieToken = charlie.accessToken();
        charlieId = charlie.userId();

        AuthResponse deactivated = register("spf_deactivated", "Alice Deactivated", "spf_deactivated@uni.edu", "dev-spf-deact");
        deactivatedToken = deactivated.accessToken();
        deactivatedId = deactivated.userId();

        // Deactivate the fourth user
        mockMvc.perform(post("/api/v1/users/me/deactivate")
                        .header("Authorization", "Bearer " + deactivatedToken))
                .andExpect(status().isNoContent());

        // Set Bob as private
        mockMvc.perform(patch("/api/v1/users/me/privacy")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdatePrivacyRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPrivate").value(true));

        // Alice creates a PUBLIC community; Bob joins it
        publicCommunityId = createCommunity(aliceToken, "SPF Public Study", CommunityVisibility.PUBLIC);
        mockMvc.perform(post("/api/v1/communities/" + publicCommunityId + "/members")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isCreated());

        // Bob creates a PRIVATE community
        privateCommunityId = createCommunity(bobToken, "SPF Bob Secret Study", CommunityVisibility.PRIVATE);

        // Bob posts check-in in PUBLIC community and in PRIVATE community
        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(publicCommunityId, "Bob public checkin", List.of()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(privateCommunityId, "Bob private community checkin", List.of()))))
                .andExpect(status().isCreated());

        // Charlie posts check-in in PUBLIC community
        UUID charlieCommId = createCommunity(charlieToken, "SPF Charlie Study", CommunityVisibility.PUBLIC);
        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(charlieCommId, "Charlie public checkin", List.of()))))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("Profile search matches name and @username prefixes case-insensitively and excludes inactive users")
    void searchUsers() throws Exception {
        // Search by name prefix (case-insensitive) -> matches Alice Searchable, excludes Alice Deactivated
        MvcResult nameRes = mockMvc.perform(get("/api/v1/users/search")
                        .param("q", "alice")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();
        List<UserSearchItemResponse> nameItems = objectMapper.readValue(
                nameRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        assertThat(nameItems).extracting(UserSearchItemResponse::id)
                .contains(aliceId)
                .doesNotContain(deactivatedId);

        // Search by @username prefix
        MvcResult handleRes = mockMvc.perform(get("/api/v1/users/search")
                        .param("q", "@SPF_BOB")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        List<UserSearchItemResponse> handleItems = objectMapper.readValue(
                handleRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        assertThat(handleItems).hasSize(1);
        assertThat(handleItems.get(0).id()).isEqualTo(bobId);
        assertThat(handleItems.get(0).isPrivate()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Private profile and check-ins are blocked before follow acceptance")
    void privateProfileBlockedBeforeFollow() throws Exception {
        // Alice views Bob's private profile -> basic info only (no institution, stats zeroed)
        MvcResult profileRes = mockMvc.perform(get("/api/v1/users/" + bobId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        UserProfileResponse bobProfile = objectMapper.readValue(profileRes.getResponse().getContentAsString(), UserProfileResponse.class);
        assertThat(bobProfile.isPrivate()).isTrue();
        assertThat(bobProfile.username()).isEqualTo("spf_bob");
        assertThat(bobProfile.institution()).isNull();
        assertThat(bobProfile.checkinsCount()).isZero();

        // Alice tries to view Bob's check-ins -> 403 PRIVATE_PROFILE
        mockMvc.perform(get("/api/v1/users/" + bobId + "/checkins")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PRIVATE_PROFILE"));

        // Alice views Charlie's public profile & check-ins -> allowed
        MvcResult charlieProfileRes = mockMvc.perform(get("/api/v1/users/" + charlieId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        UserProfileResponse charlieProfile = objectMapper.readValue(charlieProfileRes.getResponse().getContentAsString(), UserProfileResponse.class);
        assertThat(charlieProfile.isPrivate()).isFalse();
        assertThat(charlieProfile.institution()).isEqualTo("USP");
        assertThat(charlieProfile.email()).isNull();

        mockMvc.perform(get("/api/v1/users/" + charlieId + "/checkins")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @Order(4)
    @DisplayName("Follow public user succeeds immediately; follow private user creates request and supports reject/accept")
    void followWorkflow() throws Exception {
        // Alice follows Charlie (public) -> FOLLOWING immediately
        MvcResult followCharlie = mockMvc.perform(post("/api/v1/follows/" + charlieId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        FollowActionResponse charlieAction = objectMapper.readValue(followCharlie.getResponse().getContentAsString(), FollowActionResponse.class);
        assertThat(charlieAction.status()).isEqualTo(FollowRelationshipStatus.FOLLOWING);

        // Alice follows Bob (private) -> REQUESTED
        MvcResult followBob = mockMvc.perform(post("/api/v1/follows/" + bobId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        FollowActionResponse bobAction = objectMapper.readValue(followBob.getResponse().getContentAsString(), FollowActionResponse.class);
        assertThat(bobAction.status()).isEqualTo(FollowRelationshipStatus.REQUESTED);

        // Status check from Alice -> REQUESTED, followersCount = 0
        MvcResult statusRes = mockMvc.perform(get("/api/v1/follows/" + bobId + "/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        FollowStatusResponse statusBody = objectMapper.readValue(statusRes.getResponse().getContentAsString(), FollowStatusResponse.class);
        assertThat(statusBody.status()).isEqualTo(FollowRelationshipStatus.REQUESTED);
        assertThat(statusBody.followersCount()).isZero();

        // Bob lists pending requests
        MvcResult pendingRes = mockMvc.perform(get("/api/v1/follows/requests/pending")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();
        List<PendingFollowRequestResponse> pending = objectMapper.readValue(
                pendingRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        assertThat(pending).hasSize(1);
        pendingRequestId = pending.get(0).id();

        // Bob rejects request -> status becomes NONE
        mockMvc.perform(post("/api/v1/follows/requests/" + pendingRequestId + "/reject")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/follows/" + bobId + "/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.followersCount").value(0));

        // Alice re-requests -> REQUESTED again
        mockMvc.perform(post("/api/v1/follows/" + bobId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"));

        // Bob accepts request -> FOLLOWING
        mockMvc.perform(post("/api/v1/follows/requests/" + pendingRequestId + "/accept")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/follows/" + bobId + "/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FOLLOWING"))
                .andExpect(jsonPath("$.followersCount").value(1));
    }

    @Test
    @Order(5)
    @DisplayName("Accepted follower can see Bob's full public profile, public check-ins, and feed includes followed users")
    void acceptedFollowerVisibilityAndFeed() throws Exception {
        // Alice can now see Bob's full public profile (without email)
        MvcResult profileRes = mockMvc.perform(get("/api/v1/users/" + bobId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        UserProfileResponse bobProfile = objectMapper.readValue(profileRes.getResponse().getContentAsString(), UserProfileResponse.class);
        assertThat(bobProfile.institution()).isEqualTo("USP");
        assertThat(bobProfile.email()).isNull();
        assertThat(bobProfile.checkinsCount()).isEqualTo(2);

        // Alice sees Bob's public community check-in (not private community check-in)
        MvcResult checkinsRes = mockMvc.perform(get("/api/v1/users/" + bobId + "/checkins")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        List<FeedItemResponse> bobCheckins = objectMapper.readValue(
                checkinsRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        assertThat(bobCheckins).extracting(FeedItemResponse::caption)
                .containsExactly("Bob public checkin");

        // Bob himself sees both his public and private community check-ins
        MvcResult selfCheckinsRes = mockMvc.perform(get("/api/v1/users/" + bobId + "/checkins")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();
        List<FeedItemResponse> selfCheckins = objectMapper.readValue(
                selfCheckinsRes.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        assertThat(selfCheckins).hasSize(2);

        // Alice's feed includes both Charlie's post (followed public) and Bob's post (followed private + community member)
        MvcResult feedRes = mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andReturn();
        FeedResponse feed = objectMapper.readValue(feedRes.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(feed.content()).extracting(FeedItemResponse::caption)
                .contains("Charlie public checkin", "Bob public checkin")
                .doesNotContain("Bob private community checkin");
    }

    @Test
    @Order(6)
    @DisplayName("Unfollow removes follow relationship and returns NONE")
    void unfollowUser() throws Exception {
        mockMvc.perform(delete("/api/v1/follows/" + bobId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"));

        mockMvc.perform(get("/api/v1/follows/" + bobId + "/status")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.followersCount").value(0));

        // Private check-ins are blocked again
        mockMvc.perform(get("/api/v1/users/" + bobId + "/checkins")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden());
    }

    private AuthResponse register(String username, String name, String email, String deviceId) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                username, name, email, "pass123456",
                                LocalDate.of(2000, 1, 1),
                                "USP", "Computação", "Meta", "Bio", deviceId
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
    }

    private UUID createCommunity(String token, String name, CommunityVisibility visibility) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                name, "Descrição", null, visibility, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), CommunityResponse.class).id();
    }
}
