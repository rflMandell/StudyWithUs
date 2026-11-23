package com.studywithus.media;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.community.presentation.UpdateCommunityCoverRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.identity.presentation.UpdateAvatarRequest;
import com.studywithus.media.application.MediaProcessingEventListener;
import com.studywithus.media.domain.MediaConfirmedEvent;
import com.studywithus.media.domain.MediaPurpose;
import com.studywithus.media.domain.MediaStatus;
import com.studywithus.media.presentation.MediaAssetResponse;
import com.studywithus.media.presentation.RequestUploadUrlRequest;
import com.studywithus.media.presentation.UploadUrlResponse;
import com.studywithus.moderation.domain.ModerationAction;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.moderation.presentation.CreateReportRequest;
import com.studywithus.moderation.presentation.ReportResponse;
import com.studywithus.moderation.presentation.ResolveReportRequest;
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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MediaPipelineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MediaProcessingEventListener processingListener;

    private static String ownerToken;
    private static UUID ownerId;
    private static String otherUserToken;
    private static UUID communityId;
    private static UUID checkinMediaId;
    private static String checkinStorageKey;

    private static byte[] createSamplePng(int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @Order(1)
    @DisplayName("Setup: Register users and create community")
    void setup() throws Exception {
        MvcResult ownerRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "media_owner", "Media Owner", "media_owner@uni.edu", "password123",
                                LocalDate.of(2000, 1, 1), "USP", "CC", "Estudar", "Bio", "dev-1"))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse ownerAuth = objectMapper.readValue(ownerRes.getResponse().getContentAsString(), AuthResponse.class);
        ownerToken = ownerAuth.accessToken();
        ownerId = ownerAuth.userId();

        MvcResult otherRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "media_other", "Media Other", "media_other@uni.edu", "password123",
                                LocalDate.of(2000, 2, 2), "USP", "CC", "Estudar", "Bio", "dev-2"))))
                .andExpect(status().isCreated())
                .andReturn();
        otherUserToken = objectMapper.readValue(otherRes.getResponse().getContentAsString(), AuthResponse.class).accessToken();

        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                "Media Study Group", "Testing media", null, CommunityVisibility.PUBLIC, "POINTS"))))
                .andExpect(status().isCreated())
                .andReturn();
        communityId = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class).id();
    }

    @Test
    @Order(2)
    @DisplayName("Rejects invalid file extension and spoofed MIME type")
    void rejectsSpoofedUpload() throws Exception {
        // 1. Invalid extension
        mockMvc.perform(post("/api/v1/uploads")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestUploadUrlRequest(
                                MediaPurpose.CHECKIN, "script.exe", "image/png", 100, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_EXTENSION"));

        // 2. Spoofed PNG containing plain text
        byte[] fakePng = "not-really-a-png-file-content".getBytes();
        MvcResult reqRes = mockMvc.perform(post("/api/v1/uploads")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestUploadUrlRequest(
                                MediaPurpose.CHECKIN, "fake.png", "image/png", fakePng.length, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UploadUrlResponse upload = objectMapper.readValue(reqRes.getResponse().getContentAsString(), UploadUrlResponse.class);

        URI uri = URI.create(upload.presignedUrl());
        mockMvc.perform(put(uri.getPath() + "?" + uri.getQuery())
                        .contentType("image/png")
                        .content(fakePng))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/uploads/" + upload.mediaId() + "/confirm")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_MEDIA_MIME_TYPE"));
    }

    @Test
    @Order(3)
    @DisplayName("Full upload, confirm, async processing, thumbnail generation, and check-in association")
    void fullCheckinMediaFlow() throws Exception {
        byte[] pngBytes = createSamplePng(800, 600);

        MvcResult reqRes = mockMvc.perform(post("/api/v1/uploads")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestUploadUrlRequest(
                                MediaPurpose.CHECKIN, "notes.png", "image/png", pngBytes.length, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UploadUrlResponse upload = objectMapper.readValue(reqRes.getResponse().getContentAsString(), UploadUrlResponse.class);
        checkinMediaId = upload.mediaId();
        checkinStorageKey = upload.storageKey();

        // Tampered signature must fail
        URI uri = URI.create(upload.presignedUrl());
        mockMvc.perform(put(uri.getPath() + "?expires=9999999999&signature=tampered")
                        .contentType("image/png")
                        .content(pngBytes))
                .andExpect(status().isForbidden());

        // Valid presigned PUT
        mockMvc.perform(put(uri.getPath() + "?" + uri.getQuery())
                        .contentType("image/png")
                        .content(pngBytes))
                .andExpect(status().isOk());

        // Other user cannot confirm owner's upload
        mockMvc.perform(post("/api/v1/uploads/" + checkinMediaId + "/confirm")
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());

        // Owner confirms upload
        mockMvc.perform(post("/api/v1/uploads/" + checkinMediaId + "/confirm")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.detectedMimeType").value("image/png"));

        // Ensure async processing completes deterministically for assertion
        processingListener.processMedia(new MediaConfirmedEvent(checkinMediaId, ownerId, checkinStorageKey, "image/png"));

        MvcResult assetRes = mockMvc.perform(get("/api/v1/uploads/" + checkinMediaId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MediaStatus.PROCESSED.name()))
                .andExpect(jsonPath("$.width").value(800))
                .andExpect(jsonPath("$.height").value(600))
                .andExpect(jsonPath("$.thumbnailUrl").isNotEmpty())
                .andReturn();

        MediaAssetResponse asset = objectMapper.readValue(assetRes.getResponse().getContentAsString(), MediaAssetResponse.class);
        assertThat(asset.thumbnailKey()).startsWith("thumbnails/");

        // Public media & thumbnail endpoints serve bytes
        URI publicUri = URI.create(asset.url());
        mockMvc.perform(get(publicUri.getPath())).andExpect(status().isOk());
        URI thumbUri = URI.create(asset.thumbnailUrl());
        mockMvc.perform(get(thumbUri.getPath())).andExpect(status().isOk());

        // Other user cannot associate owner's storageKey to a check-in
        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId, "Stealing key", List.of(checkinStorageKey)))))
                .andExpect(status().isForbidden());

        // Owner associates media to check-in
        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId, "Resumo de Cálculo", List.of(checkinStorageKey)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaKeys[0]").value(checkinStorageKey));
    }

    @Test
    @Order(4)
    @DisplayName("Avatar and Community Cover upload association")
    void avatarAndCommunityCoverFlow() throws Exception {
        byte[] pngBytes = createSamplePng(200, 200);

        // Avatar upload
        MvcResult avatarReq = mockMvc.perform(post("/api/v1/uploads")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestUploadUrlRequest(
                                MediaPurpose.AVATAR, "avatar.png", "image/png", pngBytes.length, null))))
                .andExpect(status().isCreated())
                .andReturn();
        UploadUrlResponse avatarUpload = objectMapper.readValue(avatarReq.getResponse().getContentAsString(), UploadUrlResponse.class);
        URI avatarUri = URI.create(avatarUpload.presignedUrl());
        mockMvc.perform(put(avatarUri.getPath() + "?" + avatarUri.getQuery()).contentType("image/png").content(pngBytes))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/uploads/" + avatarUpload.mediaId() + "/confirm")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/users/me/avatar/upload")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAvatarRequest(avatarUpload.mediaId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(org.hamcrest.Matchers.containsString(avatarUpload.storageKey())));

        // Community cover upload (non-admin rejected)
        mockMvc.perform(post("/api/v1/uploads")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestUploadUrlRequest(
                                MediaPurpose.COMMUNITY_COVER, "cover.png", "image/png", pngBytes.length, communityId))))
                .andExpect(status().isForbidden());

        // Community cover upload (owner succeeds)
        MvcResult coverReq = mockMvc.perform(post("/api/v1/uploads")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestUploadUrlRequest(
                                MediaPurpose.COMMUNITY_COVER, "cover.png", "image/png", pngBytes.length, communityId))))
                .andExpect(status().isCreated())
                .andReturn();
        UploadUrlResponse coverUpload = objectMapper.readValue(coverReq.getResponse().getContentAsString(), UploadUrlResponse.class);
        URI coverUri = URI.create(coverUpload.presignedUrl());
        mockMvc.perform(put(coverUri.getPath() + "?" + coverUri.getQuery()).contentType("image/png").content(pngBytes))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/uploads/" + coverUpload.mediaId() + "/confirm")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/communities/" + communityId + "/cover/upload")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCommunityCoverRequest(coverUpload.mediaId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.containsString(coverUpload.storageKey())));
    }

    @Test
    @Order(5)
    @DisplayName("Moderation removes offensive media and blocks public access")
    void moderationRemovesOffensiveMedia() throws Exception {
        MvcResult reportRes = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateReportRequest(
                                ReportTargetType.MEDIA, checkinMediaId, "INAPPROPRIATE_IMAGE", "Offensive image"))))
                .andExpect(status().isCreated())
                .andReturn();
        ReportResponse report = objectMapper.readValue(reportRes.getResponse().getContentAsString(), ReportResponse.class);

        mockMvc.perform(post("/api/v1/reports/" + report.id() + "/resolve")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResolveReportRequest(
                                ModerationAction.REMOVE_CONTENT, "Confirmed offensive"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/uploads/" + checkinMediaId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/v1/media/public/" + checkinStorageKey))
                .andExpect(status().isNotFound());
    }
}
