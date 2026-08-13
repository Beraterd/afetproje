package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.entity.District;
import com.afet.koordinasyon.dto.response.Building3dConfigResponse;
import com.afet.koordinasyon.dto.response.BuildingDetailResponse;
import com.afet.koordinasyon.dto.response.BuildingLatestAssessmentSummary;
import com.afet.koordinasyon.ratelimit.ClientIpResolver;
import com.afet.koordinasyon.ratelimit.RateLimitFilter;
import com.afet.koordinasyon.ratelimit.RateLimiterService;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.CustomUserDetailsService;
import com.afet.koordinasyon.security.DemoModeWriteGuardFilter;
import com.afet.koordinasyon.security.JwtAuthenticationEntryPoint;
import com.afet.koordinasyon.security.JwtAuthenticationFilter;
import com.afet.koordinasyon.security.JwtTokenProvider;
import com.afet.koordinasyon.security.SecurityConfig;
import com.afet.koordinasyon.service.BuildingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bina API'sinin (1) yalnızca kimlik doğrulanmış kullanıcılara açık olduğunu, (2) admin import
 * uç noktasının yalnızca ADMIN'e açık olduğunu, ve (3) bina detay yanıtının vatandaş kişisel
 * verisi (isim/telefon/e-posta) İÇERMEDİĞİNİ doğrular — bkz. plan: graceful-coalescing-stearns.md
 * constraint 20/PII notu.
 */
@WebMvcTest(controllers = { BuildingController.class, AdminBuildingController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        DemoModeWriteGuardFilter.class, JwtTokenProvider.class, CustomUserDetailsService.class,
        RateLimitFilter.class, RateLimiterService.class, ClientIpResolver.class })
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-unit-tests-minimum-256-bits-long-000000",
        "app.jwt.access-token-expiration-ms=3600000",
        "app.jwt.refresh-token-expiration-ms=2592000000"
})
class BuildingControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private BuildingService buildingService;
    @MockBean private com.afet.koordinasyon.service.BuildingImportService buildingImportService;
    @MockBean private DistrictRepository districtRepository;
    @MockBean private UserRepository userRepository;

    @Test
    void anonymousCannotListBuildings() throws Exception {
        mockMvc.perform(get("/api/buildings").param("districtId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void authenticatedVolunteerCanListBuildings() throws Exception {
        when(buildingService.listBuildings(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of("type", "FeatureCollection", "features", List.of()));
        mockMvc.perform(get("/api/buildings").param("districtId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotTriggerImport() throws Exception {
        mockMvc.perform(post("/api/admin/buildings/import/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void volunteerCannotTriggerImport() throws Exception {
        mockMvc.perform(post("/api/admin/buildings/import/" + UUID.randomUUID())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanTriggerImport() throws Exception {
        UUID districtId = UUID.randomUUID();
        District district = District.builder().id(districtId).name("Kartal").build();
        when(districtRepository.findById(districtId)).thenReturn(java.util.Optional.of(district));
        when(buildingImportService.importFromClasspath(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("Kartal")))
                .thenReturn(new com.afet.koordinasyon.service.BuildingImportService.ImportResult(1, 0, 0, 0, List.of()));
        mockMvc.perform(post("/api/admin/buildings/import/" + districtId)).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void buildingDetailResponseContainsNoCitizenPii() throws Exception {
        UUID buildingId = UUID.randomUUID();
        BuildingDetailResponse response = BuildingDetailResponse.builder()
                .id(buildingId)
                .source("OPENSTREETMAP")
                .districtId(UUID.randomUUID())
                .districtName("Pendik")
                .neighborhoodId(UUID.randomUUID())
                .neighborhoodName("Kurtköy")
                .displayAddress("Test Sokak 1, Kurtköy Mah., Pendik/İstanbul")
                .latitude(40.9)
                .longitude(29.3)
                .estimatedHeightM(9.0)
                .isEstimatedHeight(true)
                .heightSource("levels_x3")
                .damageStatus("HEAVY")
                .damageStatusLabel("Ağır Hasar")
                .damageAssessmentCount(2)
                .verifiedAssessmentCount(1)
                .latestAssessment(BuildingLatestAssessmentSummary.builder()
                        .id(UUID.randomUUID())
                        .damageLevel("HEAVY")
                        .damageLevelLabel("Ağır Hasar")
                        .verificationStatus("SAHADA_DOGRULANDI")
                        .verificationStatusLabel("Sahada Doğrulandı")
                        .createdAt(OffsetDateTime.now())
                        .photoUrls(List.of("https://example.com/photo1"))
                        .build())
                .build();
        when(buildingService.getById(buildingId)).thenReturn(response);

        mockMvc.perform(get("/api/buildings/" + buildingId))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("reportedBy"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("verifiedBy"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("phone"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsStringIgnoringCase("email"))));
    }

    @Test
    @WithMockUser(roles = "VOLUNTEER")
    void configEndpointIsAccessible() throws Exception {
        when(buildingService.getConfig()).thenReturn(Building3dConfigResponse.builder()
                .enabledDistricts(List.of("Pendik")).buildingMinZoom(15).extrusionMinZoom(16).build());
        mockMvc.perform(get("/api/buildings/config")).andExpect(status().isOk());
    }
}
