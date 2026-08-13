package com.afet.koordinasyon.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /actuator/** yetkilendirmesini canlı Postgres'e karşı doğrular (diğer *IT sınıfları gibi —
 * çalıştırmak için `docker compose up` ile backend/docker-compose.yml'deki Postgres ayakta
 * olmalı).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSecurityIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void metricsAreNotAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void metricsAreNotAccessibleToNonAdmin() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void metricsAreAccessibleToAdmin() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk());
    }
}
