package com.studywithus.league;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.gamification.domain.BadgeConstants;
import com.studywithus.gamification.domain.BadgeRepository;
import com.studywithus.gamification.domain.UserBadge;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
import com.studywithus.league.application.CloseLeagueSeasonUseCase;
import com.studywithus.league.application.RecordLeagueScoreUseCase;
import com.studywithus.league.domain.LeagueDivision;
import com.studywithus.league.domain.LeagueOutcome;
import com.studywithus.league.domain.LeaguePool;
import com.studywithus.league.domain.LeagueRepository;
import com.studywithus.league.domain.LeagueSeasonHistory;
import com.studywithus.league.domain.UserLeagueState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
class CompetitiveLeaguesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private RecordLeagueScoreUseCase recordLeagueScoreUseCase;

    @Autowired
    private CloseLeagueSeasonUseCase closeLeagueSeasonUseCase;

    @Autowired
    private BadgeRepository badgeRepository;

    @Test
    @DisplayName("Sprint 23 End-to-End: 30-user Silver pool consolidates, promotes Top 5 to Gold, relegates Bottom 5 to Bronze, records history, awards badges, and opens next week")
    void shouldCloseWeeklySeasonPromoteTop5RelegateBottom5RecordHistoryAndOpenNewWeek() throws Exception {
        String prefix = UUID.randomUUID().toString().substring(0, 5);
        List<AuthResponse> users = new ArrayList<>(30);

        // Register 30 users and place them in SILVER division
        for (int i = 0; i < 30; i++) {
            String uname = "lg_" + prefix + "_" + i;
            MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest(
                                    uname,
                                    "League User " + i,
                                    uname + "@uni.edu",
                                    "pass123456",
                                    LocalDate.of(2001, 1, 1),
                                    "USP", "Computação", "Meta", "Bio", "dev-" + uname
                            ))))
                    .andExpect(status().isCreated())
                    .andReturn();
            AuthResponse auth = objectMapper.readValue(res.getResponse().getContentAsString(), AuthResponse.class);
            users.add(auth);

            leagueRepository.saveUserState(UserLeagueState.builder()
                    .userId(auth.userId())
                    .currentDivision(LeagueDivision.SILVER)
                    .highestDivision(LeagueDivision.SILVER)
                    .updatedAt(Instant.now())
                    .build());

            // Give descending scores: user 0 gets 300, user 1 gets 290, ..., user 29 gets 10
            long score = (30L - i) * 10L;
            recordLeagueScoreUseCase.execute(auth.userId(), score);
        }

        // Verify all 30 users fit into 1 pool of size 30
        UUID firstPoolId = leagueRepository.findParticipant(
                leagueRepository.findActiveSeason().orElseThrow().getId(),
                users.get(0).userId()
        ).orElseThrow().getPoolId();

        LeaguePool pool = leagueRepository.findPoolById(firstPoolId).orElseThrow();
        assertThat(pool.getParticipantsCount()).isEqualTo(30);
        assertThat(pool.getDivision()).isEqualTo(LeagueDivision.SILVER);

        // Close weekly season
        CloseLeagueSeasonUseCase.SeasonCloseSummary summary = closeLeagueSeasonUseCase.execute();
        assertThat(summary.totalParticipantsProcessed()).isGreaterThanOrEqualTo(30);
        assertThat(summary.promotedCount()).isGreaterThanOrEqualTo(5);
        assertThat(summary.relegatedCount()).isGreaterThanOrEqualTo(5);

        // Top 5 (users 0..4) promoted to GOLD
        for (int i = 0; i < 5; i++) {
            UUID uid = users.get(i).userId();
            UserLeagueState state = leagueRepository.findUserState(uid).orElseThrow();
            assertThat(state.getCurrentDivision()).isEqualTo(LeagueDivision.GOLD);
            assertThat(state.getHighestDivision()).isEqualTo(LeagueDivision.GOLD);

            List<LeagueSeasonHistory> history = leagueRepository.findHistoryByUserId(uid);
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getOutcome()).isEqualTo(LeagueOutcome.PROMOTED);
            assertThat(history.get(0).getDivision()).isEqualTo(LeagueDivision.SILVER);
            assertThat(history.get(0).getNextDivision()).isEqualTo(LeagueDivision.GOLD);
            assertThat(history.get(0).getFinalRank()).isEqualTo(i + 1);
        }

        // Rank #1 user receives both Campeão da Semana and Liga Ouro badges
        List<UserBadge> championBadges = badgeRepository.findUserBadges(users.get(0).userId());
        assertThat(championBadges).extracting(UserBadge::getBadgeId)
                .contains(BadgeConstants.BADGE_LEAGUE_CHAMPION_ID, BadgeConstants.BADGE_LEAGUE_GOLD_ID);

        // Middle user (index 15) stayed in SILVER
        UserLeagueState midState = leagueRepository.findUserState(users.get(15).userId()).orElseThrow();
        assertThat(midState.getCurrentDivision()).isEqualTo(LeagueDivision.SILVER);

        // Bottom 5 (users 25..29) relegated to BRONZE
        for (int i = 25; i < 30; i++) {
            UUID uid = users.get(i).userId();
            UserLeagueState state = leagueRepository.findUserState(uid).orElseThrow();
            assertThat(state.getCurrentDivision()).isEqualTo(LeagueDivision.BRONZE);

            List<LeagueSeasonHistory> history = leagueRepository.findHistoryByUserId(uid);
            assertThat(history).hasSize(1);
            assertThat(history.get(0).getOutcome()).isEqualTo(LeagueOutcome.RELEGATED);
            assertThat(history.get(0).getNextDivision()).isEqualTo(LeagueDivision.BRONZE);
        }

        // New week's endpoints reflect new season, new division, reset weeklyScore=0, and history
        mockMvc.perform(get("/api/v1/leagues/me")
                        .header("Authorization", "Bearer " + users.get(0).accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentDivision").value("GOLD"))
                .andExpect(jsonPath("$.myWeeklyScore").value(0));

        mockMvc.perform(get("/api/v1/leagues/me/history")
                        .header("Authorization", "Bearer " + users.get(0).accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].outcome").value("PROMOTED"))
                .andExpect(jsonPath("$[0].nextDivision").value("GOLD"));

        mockMvc.perform(get("/api/v1/leagues/divisions")
                        .header("Authorization", "Bearer " + users.get(0).accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }
}
