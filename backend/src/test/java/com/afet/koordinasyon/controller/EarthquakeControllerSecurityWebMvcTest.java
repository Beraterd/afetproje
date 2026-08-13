package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.ratelimit.ClientIpResolver;
import com.afet.koordinasyon.ratelimit.RateLimitFilter;
import com.afet.koordinasyon.ratelimit.RateLimiterService;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.CustomUserDetailsService;
import com.afet.koordinasyon.security.DemoModeWriteGuardFilter;
import com.afet.koordinasyon.security.JwtAuthenticationEntryPoint;
import com.afet.koordinasyon.security.JwtAuthenticationFilter;
import com.afet.koordinasyon.security.JwtTokenProvider;
import com.afet.koordinasyon.security.SecurityConfig;
import com.afet.koordinasyon.service.AfadEarthquakePollingService;
import com.afet.koordinasyon.service.EarthquakeEventService;
import com.afet.koordinasyon.service.KandilliEarthquakePollingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Item 20 (test #17) — Kandilli entegrasyonu eklendikten sonra mevcut yetkilendirme
 * politikasının AYNI kaldığını doğrular: liste/sağlayıcı uçları herkese açık (authenticated),
 * senkronizasyon uçları (AFAD ve Kandilli ikisi de) yalnızca ADMIN.
 */
@WebMvcTest(controllers = EarthquakeController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        DemoModeWriteGuardFilter.class, JwtTokenProvider.class, CustomUserDetailsService.class,
        RateLimitFilter.class, RateLimiterService.class, ClientIpResolver.class })
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-unit-tests-minimum-256-bits-long-000000",
        "app.jwt.access-token-expiration-ms=3600000",
        "app.jwt.refresh-token-expiration-ms=2592000000"
})
class EarthquakeControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EarthquakeEventService earthquakeEventService;
    @MockBean private AfadEarthquakePollingService afadPollingService;
    @MockBean private KandilliEarthquakePollingService kandilliPollingService;
    @MockBean private UserRepository userRepository;

    @Test
    void anonymousCannotListEarthquakes() throws Exception {
        mockMvc.perform(get("/api/earthquakes")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void authenticatedVolunteerCanListEarthquakes() throws Exception {
        mockMvc.perform(get("/api/earthquakes")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void authenticatedVolunteerCanListProvinces() throws Exception {
        mockMvc.perform(get("/api/earthquakes/provinces")).andExpect(status().isOk());
    }

    @Test
    void anonymousCannotTriggerAfadSync() throws Exception {
        mockMvc.perform(post("/api/earthquakes/sync")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void volunteerCannotTriggerKandilliSync() throws Exception {
        mockMvc.perform(post("/api/earthquakes/sync/kandilli")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanTriggerKandilliSync() throws Exception {
        mockMvc.perform(post("/api/earthquakes/sync/kandilli")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void volunteerCannotAccessDebugStatus() throws Exception {
        mockMvc.perform(get("/api/earthquakes/debug/status")).andExpect(status().isForbidden());
    }
}
