package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.dto.request.LoginRequest;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.DistrictRepository;
import com.afet.koordinasyon.repository.NeighborhoodRepository;
import com.afet.koordinasyon.repository.UserRepository;
import com.afet.koordinasyon.security.JwtTokenProvider;
import com.afet.koordinasyon.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private NeighborhoodRepository neighborhoodRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AuditLogService auditLogService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private HttpServletRequest httpRequest;
    private User user;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        httpRequest = mock(HttpServletRequest.class);

        UUID userId = UUID.randomUUID();
        user = User.builder().id(userId).firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").role(UserRole.VOLUNTEER).active(true).demo(false).build();
        principal = UserPrincipal.create(user);
    }

    @Test
    void login_issuesAccessAndRefreshToken() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(principal)).thenReturn("access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3_600_000L);
        when(userRepository.getReferenceById(principal.getId())).thenReturn(user);
        when(userRepository.findById(principal.getId())).thenReturn(Optional.of(user));

        var issuedToken = new RefreshTokenService.IssuedToken("raw-refresh-token", null);
        when(refreshTokenService.issue(eq(user), eq(httpRequest))).thenReturn(issuedToken);

        LoginRequest request = new LoginRequest();
        request.setEmailOrUsername("ada@example.com");
        request.setPassword("password123");

        AuthService.LoginResult result = authService.login(request, httpRequest);

        assertThat(result.response().getToken()).isEqualTo("access-token");
        assertThat(result.rawRefreshToken()).isEqualTo("raw-refresh-token");
        verify(refreshTokenService).issue(eq(user), eq(httpRequest));
        verify(auditLogService).logUserAction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void refresh_withValidToken_returnsNewAccessToken() {
        var entity = com.afet.koordinasyon.domain.entity.RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        var issuedToken = new RefreshTokenService.IssuedToken("new-raw-refresh-token", entity);
        when(refreshTokenService.rotate(eq("old-raw-token"), eq(httpRequest))).thenReturn(issuedToken);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(any())).thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3_600_000L);

        AuthService.TokenPair pair = authService.refresh("old-raw-token", httpRequest);

        assertThat(pair.response().getAccessToken()).isEqualTo("new-access-token");
        assertThat(pair.rawRefreshToken()).isEqualTo("new-raw-refresh-token");
    }

    @Test
    void refresh_forDeactivatedUser_isRejected() {
        User deactivated = User.builder().id(user.getId()).firstName("Ada").lastName("Lovelace")
                .email("ada@example.com").role(UserRole.VOLUNTEER).active(false).demo(false).build();
        var entity = com.afet.koordinasyon.domain.entity.RefreshToken.builder()
                .id(UUID.randomUUID()).user(deactivated).tokenHash("hash")
                .expiresAt(OffsetDateTime.now().plusDays(1)).revoked(false).build();
        var issuedToken = new RefreshTokenService.IssuedToken("raw", entity);
        when(refreshTokenService.rotate(any(), any())).thenReturn(issuedToken);
        when(userRepository.findById(deactivated.getId())).thenReturn(Optional.of(deactivated));

        assertThatThrownBy(() -> authService.refresh("token", httpRequest))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void logout_revokesTheGivenRefreshToken() {
        authService.logout("some-raw-token");

        verify(refreshTokenService).revoke("some-raw-token");
    }
}
