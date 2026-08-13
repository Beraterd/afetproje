package com.afet.koordinasyon.controller;

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
import com.afet.koordinasyon.service.AuthService;
import com.afet.koordinasyon.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/auth/** yetkilendirme sınırlarını doğrular: /me authenticated gerektirir (anonim -> 401,
 * NPE/500 değil), login/register/refresh/logout ise public'tir.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, JwtAuthenticationEntryPoint.class,
        DemoModeWriteGuardFilter.class, JwtTokenProvider.class, CustomUserDetailsService.class,
        RateLimitFilter.class, RateLimiterService.class, ClientIpResolver.class })
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-key-for-unit-tests-minimum-256-bits-long-000000",
        "app.jwt.access-token-expiration-ms=3600000",
        "app.jwt.refresh-token-expiration-ms=2592000000",
        "app.security.refresh-cookie-secure=false"
})
class AuthControllerSecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private PasswordResetService passwordResetService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void meIsRejectedAnonymouslyWith401NotNpe() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutCookieIsAllowedAndNoOp() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }
}
