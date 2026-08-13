package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.response.ApproveRecommendationResponse;
import com.afet.koordinasyon.dto.response.TeamRecommendationResponse;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.CustomUserDetailsService;
import com.afet.koordinasyon.security.DemoModeWriteGuardFilter;
import com.afet.koordinasyon.security.JwtAuthenticationEntryPoint;
import com.afet.koordinasyon.security.JwtAuthenticationFilter;
import com.afet.koordinasyon.security.JwtTokenProvider;
import com.afet.koordinasyon.ratelimit.ClientIpResolver;
import com.afet.koordinasyon.ratelimit.RateLimitFilter;
import com.afet.koordinasyon.ratelimit.RateLimiterService;
import com.afet.koordinasyon.security.SecurityConfig;
import com.afet.koordinasyon.security.UserPrincipal;
import com.afet.koordinasyon.service.TeamRecommendationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/team-recommendations/** yetkilendirme kurallarını gerçek Spring Security filtre
 * zincirine karşı doğrular: AI önerisi oluşturma VOLUNTEER'a kapalı, onay/red işlemleri
 * yalnızca ADMIN/DISTRICT_COORDINATOR'a açık — AI'ın kendisi hiçbir mutasyonu insan onayı
 * olmadan tetikleyemez, bu test o güvenceyi kanıtlar.
 */
@WebMvcTest(controllers = TeamRecommendationController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        DemoModeWriteGuardFilter.class, JwtTokenProvider.class, CustomUserDetailsService.class,
        RateLimitFilter.class, RateLimiterService.class, ClientIpResolver.class })
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-unit-tests-minimum-256-bits-long-000000",
        "app.jwt.access-token-expiration-ms=3600000",
        "app.jwt.refresh-token-expiration-ms=2592000000"
})
class TeamRecommendationControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamRecommendationService recommendationService;

    @MockBean
    private UserRepository userRepository;

    private String tokenFor(UserRole role) {
        UUID id = UUID.randomUUID();
        User user = User.builder().id(id).firstName("Test").lastName("User")
                .email("test-" + id + "@example.com").role(role).active(true).demo(false).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        UserPrincipal principal = UserPrincipal.create(user);
        return jwtTokenProvider.generateAccessToken(principal);
    }

    @Test
    void anonymousCannotCreateRecommendation() throws Exception {
        mockMvc.perform(post("/api/team-recommendations")
                        .contentType("application/json")
                        .content("{\"teamType\":\"SEARCH_RESCUE\",\"districtId\":\"" + UUID.randomUUID() + "\",\"requiredTeamSize\":2}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void volunteerCannotCreateRecommendation() throws Exception {
        String token = tokenFor(UserRole.VOLUNTEER);

        mockMvc.perform(post("/api/team-recommendations")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"teamType\":\"SEARCH_RESCUE\",\"districtId\":\"" + UUID.randomUUID() + "\",\"requiredTeamSize\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void volunteerCannotApproveRecommendation() throws Exception {
        String token = tokenFor(UserRole.VOLUNTEER);

        mockMvc.perform(post("/api/team-recommendations/{id}/approve", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new com.afet.koordinasyon.dto.request.ApproveRecommendationRequest(List.of(UUID.randomUUID())))))
                .andExpect(status().isForbidden());
    }

    @Test
    void volunteerCannotApproveAllRecommendation() throws Exception {
        String token = tokenFor(UserRole.VOLUNTEER);

        mockMvc.perform(post("/api/team-recommendations/{id}/approve-all", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void volunteerCannotRejectRecommendation() throws Exception {
        String token = tokenFor(UserRole.VOLUNTEER);

        mockMvc.perform(post("/api/team-recommendations/{id}/reject", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void districtCoordinatorCanApproveRecommendation() throws Exception {
        when(recommendationService.approveWithSelectedMembers(any(), any(), any()))
                .thenReturn(ApproveRecommendationResponse.builder().build());
        String token = tokenFor(UserRole.DISTRICT_COORDINATOR);

        mockMvc.perform(post("/api/team-recommendations/{id}/approve", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new com.afet.koordinasyon.dto.request.ApproveRecommendationRequest(List.of(UUID.randomUUID())))))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanRejectRecommendation() throws Exception {
        when(recommendationService.reject(any(), any())).thenReturn(TeamRecommendationResponse.builder().build());
        String token = tokenFor(UserRole.ADMIN);

        mockMvc.perform(post("/api/team-recommendations/{id}/reject", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
