package com.studywithus.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CheckinResponse;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FeedIntegrationTest {

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
    private static UUID communityId;
    private static UUID checkin1Id;
    private static UUID checkin2Id;
    private static UUID checkin3Id;

    @Test
    @Order(1)
    @DisplayName("Setup: Register Alice, Bob and Charlie; Alice creates community; Bob joins")
    void setup() throws Exception {
        // Register Alice
        MvcResult resAlice = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "feed_alice",
                                "Alice Aluna",
                                "alice_feed@uni.edu",
                                "pass123456",
                                LocalDate.of(2001, 1, 1),
                                "USP", "Física", "Meta", "Bio", "dev-alice"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authAlice = objectMapper.readValue(resAlice.getResponse().getContentAsString(), AuthResponse.class);
        aliceToken = authAlice.accessToken();
        aliceId = authAlice.userId();

        // Register Bob
        MvcResult resBob = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "feed_bob",
                                "Bob Aluno",
                                "bob_feed@uni.edu",
                                "pass123456",
                                LocalDate.of(2002, 2, 2),
                                "Unicamp", "Computação", "Meta", "Bio", "dev-bob"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authBob = objectMapper.readValue(resBob.getResponse().getContentAsString(), AuthResponse.class);
        bobToken = authBob.accessToken();
        bobId = authBob.userId();

        // Register Charlie
        MvcResult resCharlie = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "feed_charlie",
                                "Charlie Aluno",
                                "charlie_feed@uni.edu",
                                "pass123456",
                                LocalDate.of(2003, 3, 3),
                                "UFRJ", "Matemática", "Meta", "Bio", "dev-charlie"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse authCharlie = objectMapper.readValue(resCharlie.getResponse().getContentAsString(), AuthResponse.class);
        charlieToken = authCharlie.accessToken();
        charlieId = authCharlie.userId();

        // Alice creates public community
        MvcResult resComm = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                "Comunidade Feed Teste",
                                "Comunidade para validação do Feed",
                                null,
                                CommunityVisibility.PUBLIC,
                                "America/Sao_Paulo"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        CommunityResponse commResponse = objectMapper.readValue(resComm.getResponse().getContentAsString(), CommunityResponse.class);
        communityId = commResponse.id();

        // Bob joins community
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/members")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    @DisplayName("Demonstrable test: Alice creates checkin in community -> appears in Bob's feed -> Charlie feed is empty")
    void shouldShowCheckinInPermittedMemberFeedOnly() throws Exception {
        // Alice posts checkin
        MvcResult resCheckin = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                "Primeiro check-in de estudo da Alice",
                                List.of("foto_resumo.png")
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        CheckinResponse checkinResponse = objectMapper.readValue(resCheckin.getResponse().getContentAsString(), CheckinResponse.class);
        checkin1Id = checkinResponse.id();

        // Bob (member) requests feed -> sees Alice's checkin
        MvcResult resBobFeed = mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(checkin1Id.toString()))
                .andExpect(jsonPath("$.content[0].authorUsername").value("feed_alice"))
                .andExpect(jsonPath("$.content[0].communityName").value("Comunidade Feed Teste"))
                .andExpect(jsonPath("$.content[0].caption").value("Primeiro check-in de estudo da Alice"))
                .andExpect(jsonPath("$.hasMore").value(false))
                .andReturn();

        FeedResponse bobFeed = objectMapper.readValue(resBobFeed.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(bobFeed.content()).hasSize(1);
        assertThat(bobFeed.nextCursor()).isNotNull();

        // Charlie (non-member) requests feed -> empty feed
        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @Order(3)
    @DisplayName("Cursor pagination test: Alice creates two more checkins -> Bob paginates with limit and cursor")
    void shouldPaginateFeedUsingCursor() throws Exception {
        // Alice posts checkin 2
        MvcResult resCheckin2 = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                "Segundo check-in de estudo",
                                List.of()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        checkin2Id = objectMapper.readValue(resCheckin2.getResponse().getContentAsString(), CheckinResponse.class).id();

        // Alice posts checkin 3
        MvcResult resCheckin3 = mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                "Terceiro check-in de estudo",
                                List.of()
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        checkin3Id = objectMapper.readValue(resCheckin3.getResponse().getContentAsString(), CheckinResponse.class).id();

        // Bob requests feed with limit=2 (page 1)
        MvcResult page1Result = mockMvc.perform(get("/api/v1/feed?limit=2")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.hasMore").value(true))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn();

        FeedResponse page1 = objectMapper.readValue(page1Result.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(page1.content()).hasSize(2);
        assertThat(page1.content().get(0).id()).isEqualTo(checkin3Id);
        assertThat(page1.content().get(1).id()).isEqualTo(checkin2Id);
        assertThat(page1.hasMore()).isTrue();

        String cursor = page1.nextCursor();
        assertThat(cursor).isNotBlank();

        // Bob requests feed with cursor (page 2)
        MvcResult page2Result = mockMvc.perform(get("/api/v1/feed?cursor=" + cursor + "&limit=2")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.hasMore").value(false))
                .andReturn();

        FeedResponse page2 = objectMapper.readValue(page2Result.getResponse().getContentAsString(), FeedResponse.class);
        assertThat(page2.content()).hasSize(1);
        assertThat(page2.content().get(0).id()).isEqualTo(checkin1Id);
        assertThat(page2.hasMore()).isFalse();
    }

    @Test
    @Order(4)
    @DisplayName("Soft delete check: deleted checkin disappears from feed")
    void shouldExcludeDeletedCheckinFromFeed() throws Exception {
        // Alice deletes checkin 3
        mockMvc.perform(delete("/api/v1/checkins/" + checkin3Id)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());

        // Bob requests feed -> checkin 3 should no longer be present
        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + checkin3Id + "')]").doesNotExist())
                .andExpect(jsonPath("$.content[0].id").value(checkin2Id.toString()));
    }
}
