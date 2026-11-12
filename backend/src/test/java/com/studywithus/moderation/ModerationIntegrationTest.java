package com.studywithus.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.moderation.domain.ModerationAction;
import com.studywithus.moderation.domain.ReportStatus;
import com.studywithus.moderation.domain.ReportTargetType;
import com.studywithus.moderation.presentation.CreateReportRequest;
import com.studywithus.moderation.presentation.ReportResponse;
import com.studywithus.moderation.presentation.ResolveReportRequest;
import com.studywithus.social.presentation.CommentResponse;
import com.studywithus.social.presentation.CreateCommentRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ModerationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String tokenOwner;
    private static UUID ownerId;

    private static String tokenUserReporter;
    private static UUID reporterId;

    private static String tokenUserSpammer;
    private static UUID spammerId;

    private static UUID communityId;
    private static UUID checkinId;
    private static UUID abusiveCommentId;
    private static UUID reportId;

    @Test
    @Order(1)
    @DisplayName("Setup: Register Owner, Reporter and Spammer, create community, check-in and abusive comment")
    void setup() throws Exception {
        // 1. Owner (moderator of community)
        RegisterRequest reqOwner = new RegisterRequest(
                "mod_owner_s17",
                "Community Moderator Owner",
                "mod_owner_s17@studywithus.com",
                "password123",
                LocalDate.of(1995, 5, 20),
                "USP",
                "Direito",
                "Moderar",
                "Bio Owner",
                "owner-s17"
        );
        MvcResult resOwner = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqOwner)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authOwner = objectMapper.readValue(resOwner.getResponse().getContentAsString(), AuthResponse.class);
        tokenOwner = authOwner.accessToken();
        ownerId = authOwner.userId();

        // 2. Reporter (community member)
        RegisterRequest reqReporter = new RegisterRequest(
                "reporter_user_s17",
                "Reporter User",
                "reporter_s17@studywithus.com",
                "password123",
                LocalDate.of(1998, 3, 15),
                "USP",
                "Computacao",
                "Aprender",
                "Bio Reporter",
                "reporter-s17"
        );
        MvcResult resReporter = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqReporter)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authReporter = objectMapper.readValue(resReporter.getResponse().getContentAsString(), AuthResponse.class);
        tokenUserReporter = authReporter.accessToken();
        reporterId = authReporter.userId();

        // 3. Spammer
        RegisterRequest reqSpammer = new RegisterRequest(
                "spammer_user_s17",
                "Spammer User",
                "spammer_s17@studywithus.com",
                "password123",
                LocalDate.of(2001, 8, 10),
                "USP",
                "Administracao",
                "Spam",
                "Bio Spammer",
                "spammer-s17"
        );
        MvcResult resSpammer = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqSpammer)))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authSpammer = objectMapper.readValue(resSpammer.getResponse().getContentAsString(), AuthResponse.class);
        tokenUserSpammer = authSpammer.accessToken();
        spammerId = authSpammer.userId();

        // Owner creates community
        CreateCommunityRequest commReq = new CreateCommunityRequest(
                "Moderation Test Group",
                "Test community for moderation and reports",
                null,
                CommunityVisibility.PUBLIC,
                "America/Sao_Paulo"
        );
        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CommunityResponse commResp = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class);
        communityId = commResp.id();

        // Reporter and Spammer join community
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenUserReporter))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + tokenUserSpammer))
                .andExpect(status().isCreated());

        // Owner creates check-in
        CreateCheckinRequest checkinReq = new CreateCheckinRequest(
                communityId,
                "Study session on distributed systems",
                List.of("storage/photo1.png")
        );
        MvcResult checkinRes = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkinReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CheckinResponse checkinResp = objectMapper.readValue(checkinRes.getResponse().getContentAsString(), CheckinResponse.class);
        checkinId = checkinResp.id();

        // Spammer posts abusive/spam comment
        CreateCommentRequest commentReq = new CreateCommentRequest(
                "CLICK HERE FOR FREE BITCOINS http://spam-malware.xyz DO NOT MISS OUT!"
        );
        MvcResult commentRes = mockMvc.perform(post("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserSpammer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentReq)))
                .andExpect(status().isCreated())
                .andReturn();
        CommentResponse commentResp = objectMapper.readValue(commentRes.getResponse().getContentAsString(), CommentResponse.class);
        abusiveCommentId = commentResp.id();
        assertThat(abusiveCommentId).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Step 1: Reporter flags abusive comment via POST /api/v1/reports")
    void step1_reporterFlagsAbusiveComment() throws Exception {
        CreateReportRequest reportReq = new CreateReportRequest(
                ReportTargetType.COMMENT,
                abusiveCommentId,
                "Spam and phishing links",
                "User posted an untrusted spam link asking for cryptocurrency"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + tokenUserReporter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.reporterId").value(reporterId.toString()))
                .andExpect(jsonPath("$.targetType").value("COMMENT"))
                .andExpect(jsonPath("$.targetId").value(abusiveCommentId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        ReportResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), ReportResponse.class);
        reportId = response.id();
        assertThat(reportId).isNotNull();
    }

    @Test
    @Order(3)
    @DisplayName("Step 2: Moderator lists pending reports via GET /api/v1/reports")
    void step2_moderatorListsPendingReports() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/reports")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[?(@.id == '" + reportId + "')].reason").value("Spam and phishing links"))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(root.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(4)
    @DisplayName("Step 3: Moderator inspects report by ID via GET /api/v1/reports/{id}")
    void step3_moderatorInspectsReportById() throws Exception {
        mockMvc.perform(get("/api/v1/reports/" + reportId)
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.targetType").value("COMMENT"))
                .andExpect(jsonPath("$.targetId").value(abusiveCommentId.toString()));
    }

    @Test
    @Order(5)
    @DisplayName("Step 4: Non-moderator cannot resolve report (403 Forbidden)")
    void step4_nonModeratorCannotResolveReport() throws Exception {
        ResolveReportRequest resolveReq = new ResolveReportRequest(
                ModerationAction.REMOVE_CONTENT,
                "Unauthorized resolve attempt"
        );

        mockMvc.perform(post("/api/v1/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + tokenUserSpammer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(6)
    @DisplayName("Step 5: Moderator resolves report with REMOVE_CONTENT action via POST /api/v1/reports/{id}/resolve")
    void step5_moderatorResolvesReport() throws Exception {
        ResolveReportRequest resolveReq = new ResolveReportRequest(
                ModerationAction.REMOVE_CONTENT,
                "Link contains malicious content; comment deleted and user warned"
        );

        mockMvc.perform(post("/api/v1/reports/" + reportId + "/resolve")
                        .header("Authorization", "Bearer " + tokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resolveReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reportId.toString()))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.reviewedBy").value(ownerId.toString()))
                .andExpect(jsonPath("$.reviewedAt").isNotEmpty());
    }

    @Test
    @Order(7)
    @DisplayName("Step 6: Verify abusive comment is soft-deleted and no longer appears in check-in comments")
    void step6_verifyCommentDeleted() throws Exception {
        MvcResult commentsRes = mockMvc.perform(get("/api/v1/checkins/" + checkinId + "/comments")
                        .header("Authorization", "Bearer " + tokenUserReporter))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(commentsRes.getResponse().getContentAsString());
        JsonNode content = root.get("content");
        for (JsonNode commentNode : content) {
            assertThat(commentNode.get("id").asText()).isNotEqualTo(abusiveCommentId.toString());
        }
    }

    @Test
    @Order(8)
    @DisplayName("Step 7: Verify audit_log is registered for moderation action via GET /api/v1/audit-logs")
    void step7_verifyAuditLogCreated() throws Exception {
        MvcResult auditRes = mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        JsonNode root = objectMapper.readTree(auditRes.getResponse().getContentAsString());
        JsonNode content = root.get("content");
        boolean foundAudit = false;
        for (JsonNode logNode : content) {
            if ("REPORT_RESOLVED".equals(logNode.get("action").asText())
                    && ownerId.toString().equals(logNode.get("actorId").asText())
                    && abusiveCommentId.toString().equals(logNode.get("targetId").asText())) {
                foundAudit = true;
                assertThat(logNode.get("metadata").asText()).contains("REMOVE_CONTENT");
                break;
            }
        }
        assertThat(foundAudit).isTrue();
    }

    @Test
    @Order(9)
    @DisplayName("Step 8: Dismiss report flow test")
    void step8_dismissReportFlow() throws Exception {
        // Reporter reports check-in as test
        CreateReportRequest reportReq = new CreateReportRequest(
                ReportTargetType.CHECKIN,
                checkinId,
                "Inappropriate topic",
                "Not really inappropriate, testing dismissal"
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + tokenUserReporter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportReq)))
                .andExpect(status().isCreated())
                .andReturn();

        ReportResponse createdReport = objectMapper.readValue(createRes.getResponse().getContentAsString(), ReportResponse.class);

        // Moderator dismisses the report
        mockMvc.perform(post("/api/v1/reports/" + createdReport.id() + "/dismiss")
                        .header("Authorization", "Bearer " + tokenOwner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdReport.id().toString()))
                .andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.reviewedBy").value(ownerId.toString()));
    }
}
