package com.studywithus.checkin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.infrastructure.SpringDataCheckinRepository;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.media.presentation.MediaAssetResponse;
import com.studywithus.media.presentation.RequestUploadUrlRequest;
import com.studywithus.media.presentation.UploadUrlResponse;
import com.studywithus.social.domain.Follow;
import com.studywithus.social.domain.FollowRepository;
import com.studywithus.social.presentation.CommentResponse;
import com.studywithus.social.presentation.CreateCommentRequest;
import com.studywithus.social.presentation.FeedResponse;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StandalonePostIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FollowRepository followRepository;

    @Autowired
    private SpringDataCheckinRepository checkinRepository;

    private static String authorToken;
    private static UUID authorId;
    private static String followerToken;
    private static UUID followerId;
    private static String strangerToken;
    private static UUID standaloneTextId;
    private static UUID standaloneMediaId;
    private static String uploadedMediaKey;
    private static UUID commentId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register author, follower, and stranger users")
    void setup() throws Exception {
        AuthResponse authorAuth = registerUser("standalone_author", "standalone_author@uni.edu", "dev-standalone-1");
        authorToken = authorAuth.accessToken();
        authorId = authorAuth.userId();

        AuthResponse followerAuth = registerUser("standalone_follower", "standalone_follower@uni.edu", "dev-standalone-2");
        followerToken = followerAuth.accessToken();
        followerId = followerAuth.userId();

        AuthResponse strangerAuth = registerUser("standalone_stranger", "standalone_stranger@uni.edu", "dev-standalone-3");
        strangerToken = strangerAuth.accessToken();

        followRepository.save(Follow.builder()
                .followerId(followerId)
                .followingId(authorId)
                .createdAt(Instant.now())
                .build());
    }

    @Test
    @Order(2)
    @DisplayName("Author creates a standalone post (communityId = null) with caption only")
    void createStandalonePostWithCaption() throws Exception {
        CreateCheckinRequest request = new CreateCheckinRequest(
                null,
                "Revisão de Cálculo II fora de comunidade",
                List.of()
        );

        MvcResult result = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(authorId.toString()))
                .andExpect(jsonPath("$.communityId").doesNotExist())
                .andExpect(jsonPath("$.caption").value("Revisão de Cálculo II fora de comunidade"))
                .andExpect(jsonPath("$.countsAsCheckin").value(true))
                .andReturn();

        CheckinResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CheckinResponse.class);
        standaloneTextId = response.id();
        assertThat(standaloneTextId).isNotNull();
        assertThat(response.communityId()).isNull();
        assertThat(response.countsAsCheckin()).isTrue();
    }

    @Test
    @Order(3)
    @DisplayName("Author creates a standalone post with uploaded & confirmed image")
    void createStandalonePostWithMedia() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", baos);
        byte[] pngBytes = baos.toByteArray();

        MvcResult uploadUrlRes = mockMvc.perform(post("/api/v1/uploads")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestUploadUrlRequest(
                                MediaPurpose.CHECKIN, "standalone.png", "image/png", pngBytes.length, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UploadUrlResponse uploadUrl = objectMapper.readValue(uploadUrlRes.getResponse().getContentAsString(), UploadUrlResponse.class);

        URI presignedUri = URI.create(uploadUrl.presignedUrl());
        mockMvc.perform(put(presignedUri.getPath() + "?" + presignedUri.getQuery())
                        .contentType("image/png")
                        .content(pngBytes))
                .andExpect(status().isOk());

        MvcResult confirmRes = mockMvc.perform(post("/api/v1/uploads/" + uploadUrl.mediaId() + "/confirm")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andReturn();
        MediaAssetResponse confirmedAsset = objectMapper.readValue(confirmRes.getResponse().getContentAsString(), MediaAssetResponse.class);
        uploadedMediaKey = confirmedAsset.storageKey();

        CreateCheckinRequest request = new CreateCheckinRequest(
                null,
                "Resumo ilustrado",
                List.of(uploadedMediaKey)
        );

        MvcResult result = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.communityId").doesNotExist())
                .andExpect(jsonPath("$.mediaKeys[0]").value(uploadedMediaKey))
                .andReturn();

        CheckinResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CheckinResponse.class);
        standaloneMediaId = response.id();
        assertThat(standaloneMediaId).isNotNull();
    }

    @Test
    @Order(4)
    @DisplayName("Standalone posts appear on author's profile (self and public view)")
    void standalonePostsAppearOnProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/" + authorId + "/checkins")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(standaloneMediaId.toString()))
                .andExpect(jsonPath("$[0].communityId").doesNotExist())
                .andExpect(jsonPath("$[0].communityName").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(standaloneTextId.toString()));

        mockMvc.perform(get("/api/v1/users/" + authorId + "/checkins")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(standaloneMediaId.toString()))
                .andExpect(jsonPath("$[1].id").value(standaloneTextId.toString()));
    }

    @Test
    @Order(5)
    @DisplayName("Standalone posts appear on author's feed and follower's feed, but NOT on stranger's feed")
    void standalonePostsVisibilityInFeed() throws Exception {
        MvcResult authorFeedRes = mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andReturn();
        FeedResponse authorFeed = objectMapper.readValue(authorFeedRes.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(authorFeed.content()).extracting("id").contains(standaloneTextId, standaloneMediaId);

        MvcResult followerFeedRes = mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andReturn();
        FeedResponse followerFeed = objectMapper.readValue(followerFeedRes.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(followerFeed.content()).extracting("id").contains(standaloneTextId, standaloneMediaId);

        MvcResult strangerFeedRes = mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andReturn();
        FeedResponse strangerFeed = objectMapper.readValue(strangerFeedRes.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(strangerFeed.content()).extracting("id").doesNotContain(standaloneTextId, standaloneMediaId);
    }

    @Test
    @Order(6)
    @DisplayName("Details, likes, and comments work on standalone posts for author and other users")
    void detailsLikesAndCommentsOnStandalonePost() throws Exception {
        mockMvc.perform(get("/api/v1/checkins/" + standaloneTextId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(standaloneTextId.toString()))
                .andExpect(jsonPath("$.communityId").doesNotExist());

        // Stranger likes standalone post
        mockMvc.perform(post("/api/v1/checkins/" + standaloneTextId + "/like")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likesCount").value(1));

        // Author likes standalone post
        mockMvc.perform(post("/api/v1/checkins/" + standaloneTextId + "/like")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likesCount").value(2));

        // Stranger comments on standalone post
        MvcResult commentRes = mockMvc.perform(post("/api/v1/checkins/" + standaloneTextId + "/comments")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Ótimo resumo!", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Ótimo resumo!"))
                .andReturn();
        commentId = objectMapper.readValue(commentRes.getResponse().getContentAsString(), CommentResponse.class).id();

        // Author likes comment
        mockMvc.perform(post("/api/v1/comments/" + commentId + "/like")
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.likesCount").value(1));

        // List comments
        mockMvc.perform(get("/api/v1/checkins/" + standaloneTextId + "/comments")
                        .header("Authorization", "Bearer " + followerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(commentId.toString()))
                .andExpect(jsonPath("$.content[0].likesCount").value(1));
    }

    @Test
    @Order(7)
    @DisplayName("Deleting standalone post: non-author is forbidden, author succeeds")
    void deleteStandalonePostAuthorization() throws Exception {
        mockMvc.perform(delete("/api/v1/checkins/" + standaloneTextId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CHECKIN_ACCESS_DENIED"));

        mockMvc.perform(delete("/api/v1/checkins/" + standaloneTextId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/checkins/" + standaloneTextId)
                        .header("Authorization", "Bearer " + authorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(8)
    @DisplayName("Standalone post with countsAsCheckin=false appears on profile/feed with likes/comments, but awards no points, XP, streak, or checkin count")
    void standalonePostCountsAsCheckinFalseSkipsGamification() throws Exception {
        AuthResponse postUserAuth = registerUser("non_checkin_user", "non_checkin_user@uni.edu", "dev-non-checkin-1");
        String postUserToken = postUserAuth.accessToken();
        UUID postUserId = postUserAuth.userId();

        CreateCheckinRequest request = new CreateCheckinRequest(
                null,
                "Apenas compartilhando uma reflexão de estudos",
                List.of(),
                null,
                null,
                null,
                false
        );

        MvcResult result = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + postUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countsAsCheckin").value(false))
                .andReturn();

        CheckinResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), CheckinResponse.class);
        assertThat(response.countsAsCheckin()).isFalse();
        UUID postId = response.id();

        // Appears on profile and feed with countsAsCheckin = false
        mockMvc.perform(get("/api/v1/users/" + postUserId + "/checkins")
                        .header("Authorization", "Bearer " + postUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(postId.toString()))
                .andExpect(jsonPath("$[0].countsAsCheckin").value(false));

        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + postUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(postId.toString()))
                .andExpect(jsonPath("$.content[0].countsAsCheckin").value(false));

        // Supports likes & comments
        mockMvc.perform(post("/api/v1/checkins/" + postId + "/like")
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likesCount").value(1));

        mockMvc.perform(post("/api/v1/checkins/" + postId + "/comments")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommentRequest("Boa reflexão!", null))))
                .andExpect(status().isCreated());

        // Does NOT award points, XP, or streak, and does NOT increment countActiveByUserId
        mockMvc.perform(get("/api/v1/users/me/points")
                        .header("Authorization", "Bearer " + postUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifetimePoints").value(0));

        mockMvc.perform(get("/api/v1/users/me/level")
                        .header("Authorization", "Bearer " + postUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(0));

        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + postUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(0));

        assertThat(checkinRepository.countActiveByUserId(postUserId)).isZero();

        // Creating a standalone post with countsAsCheckin=true (or omitted) awards points, updates streak, and increments countActiveByUserId
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                null,
                "Agora sim um check-in valendo!",
                List.of(),
                null,
                null,
                null,
                true
        );

        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + postUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.countsAsCheckin").value(true));

        mockMvc.perform(get("/api/v1/users/me/points")
                        .header("Authorization", "Bearer " + postUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifetimePoints").value(10));

        mockMvc.perform(get("/api/v1/users/me/streak")
                        .header("Authorization", "Bearer " + postUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(1));

        assertThat(checkinRepository.countActiveByUserId(postUserId)).isEqualTo(1L);
    }

    private AuthResponse registerUser(String username, String email, String deviceId) throws Exception {
        RegisterRequest req = new RegisterRequest(
                username,
                username,
                email,
                "password123",
                LocalDate.of(2000, 1, 1),
                "USP",
                "Computação",
                "Estudar",
                "Bio",
                deviceId
        );
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
    }
}
