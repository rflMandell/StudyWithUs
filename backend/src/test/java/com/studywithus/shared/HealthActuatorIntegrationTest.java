package com.studywithus.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthActuatorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Sprint 12: GET /actuator/health should return UP status and components")
    void healthCheckShouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Sprint 12: GET /actuator/metrics should be accessible")
    void metricsShouldBeAccessible() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }
}
