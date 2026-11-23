package com.studywithus.gamification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studywithus.checkin.presentation.CreateCheckinRequest;
import com.studywithus.community.domain.CommunityVisibility;
import com.studywithus.community.presentation.CommunityResponse;
import com.studywithus.community.presentation.CreateCommunityRequest;
import com.studywithus.gamification.presentation.EquipCosmeticRequest;
import com.studywithus.identity.presentation.AuthResponse;
import com.studywithus.identity.presentation.RegisterRequest;
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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XpAndLevelingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Sprint 22 End-to-End: Check-ins award XP, trigger level-up, send notification, unlock cosmetics, and reflect in profile and feed")
    void shouldAwardXpLevelUpSendNotificationAndReflectInProfileAndFeed() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest(
                                "xp_user_" + suffix,
                                "XP Student",
                                "xp_" + suffix + "@uni.edu",
                                "pass123456",
                                LocalDate.of(2001, 5, 10),
                                "USP", "Computação", "Meta", "Bio", "dev-xp"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(regRes.getResponse().getContentAsString(), AuthResponse.class);

        // Initial level is 1
        mockMvc.perform(get("/api/v1/users/me/level")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.activeTitle").value("Calouro"))
                .andExpect(jsonPath("$.activeAvatarBorder").value("BORDER_NONE"));

        // Create community
        MvcResult commRes = mockMvc.perform(post("/api/v1/communities")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCommunityRequest(
                                "Comunidade XP " + suffix,
                                "Estudos com XP",
                                null,
                                CommunityVisibility.PUBLIC,
                                "America/Sao_Paulo"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID communityId = objectMapper.readValue(commRes.getResponse().getContentAsString(), CommunityResponse.class).id();

        // Check-in 1 -> 50 XP (still Level 1: 50/100)
        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                "Primeiro estudo valendo XP",
                                List.of()
                        ))))
                .andExpect(status().isCreated());

        // Check-in 2 -> +50 XP = 100 XP -> Level 2 ("Estudante Dedicado", "BORDER_BRONZE")
        mockMvc.perform(post("/api/v1/checkins")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCheckinRequest(
                                communityId,
                                "Segundo estudo subindo de nível!",
                                List.of()
                        ))))
                .andExpect(status().isCreated());

        // Verify level endpoint
        mockMvc.perform(get("/api/v1/users/me/level")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.totalXp").value(100))
                .andExpect(jsonPath("$.currentLevelXp").value(0))
                .andExpect(jsonPath("$.nextLevelXp").value(200))
                .andExpect(jsonPath("$.activeTitle").value("Estudante Dedicado"))
                .andExpect(jsonPath("$.activeAvatarBorder").value("BORDER_BRONZE"));

        // Verify profile reflects level, progress, title, and border
        mockMvc.perform(get("/api/v1/users/" + auth.userId())
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.totalXp").value(100))
                .andExpect(jsonPath("$.activeTitle").value("Estudante Dedicado"))
                .andExpect(jsonPath("$.activeAvatarBorder").value("BORDER_BRONZE"));

        // Verify feed author card reflects level, title, and border
        mockMvc.perform(get("/api/v1/feed")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].authorLevel").value(2))
                .andExpect(jsonPath("$.content[0].authorTitle").value("Estudante Dedicado"))
                .andExpect(jsonPath("$.content[0].authorAvatarBorder").value("BORDER_BRONZE"));

        // Verify LEVEL_UP notification was created
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].type", hasItem("LEVEL_UP")));

        // Verify cosmetic equipping: can equip Level 1 title, cannot equip Level 10 border
        mockMvc.perform(patch("/api/v1/users/me/cosmetics")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EquipCosmeticRequest("Calouro", "BORDER_BRONZE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeTitle").value("Calouro"));

        mockMvc.perform(patch("/api/v1/users/me/cosmetics")
                        .header("Authorization", "Bearer " + auth.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EquipCosmeticRequest(null, "BORDER_AURORA"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COSMETIC_LOCKED"));
    }
}
