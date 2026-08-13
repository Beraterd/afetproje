package com.afet.koordinasyon.controller;

import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.response.GlobalSearchResponse;
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
import com.afet.koordinasyon.service.SearchService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/search yetkilendirmesini gerçek Spring Security filtre zincirine karşı doğrular:
 * anonim erişemez, authenticated herhangi bir rol erişebilir (ince taneli filtre serviste).
 */
@WebMvcTest(controllers = SearchController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        DemoModeWriteGuardFilter.class, JwtTokenProvider.class, CustomUserDetailsService.class,
        RateLimitFilter.class, RateLimiterService.class, ClientIpResolver.class })
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-unit-tests-minimum-256-bits-long-000000",
        "app.jwt.access-token-expiration-ms=3600000",
        "app.jwt.refresh-token-expiration-ms=2592000000"
})
class SearchControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SearchService searchService;

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
    void anonymousCannotSearch() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "kadikoy"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void volunteerCanSearch() throws Exception {
        when(searchService.search(any(), any())).thenReturn(new GlobalSearchResponse("kadikoy", List.of()));
        String token = tokenFor(UserRole.VOLUNTEER);

        mockMvc.perform(get("/api/search").param("q", "kadikoy")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanSearch() throws Exception {
        when(searchService.search(any(), any())).thenReturn(new GlobalSearchResponse("kadikoy", List.of()));
        String token = tokenFor(UserRole.ADMIN);

        mockMvc.perform(get("/api/search").param("q", "kadikoy")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
