package com.afet.koordinasyon.service;

import com.afet.koordinasyon.domain.entity.RefreshToken;
import com.afet.koordinasyon.domain.entity.User;
import com.afet.koordinasyon.domain.enums.UserRole;
import com.afet.koordinasyon.exception.BusinessRuleException;
import com.afet.koordinasyon.repository.RefreshTokenRepository;
import com.afet.koordinasyon.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private JwtTokenProvider jwtTokenProvider;
    private RefreshTokenService refreshTokenService;

    private User user;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-for-unit-tests-minimum-256-bits-long-000000",
                3_600_000L,
                2_592_000_000L);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtTokenProvider);

        user = User.builder().id(UUID.randomUUID()).firstName("Test").lastName("User")
                .email("test@example.com").role(UserRole.VOLUNTEER).active(true).demo(false).build();

        request = mock(HttpServletRequest.class);
        lenient().when(request.getHeader("User-Agent")).thenReturn("JUnit");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void issue_persistsHashedTokenAndReturnsRawToken() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        RefreshTokenService.IssuedToken issued = refreshTokenService.issue(user, request);

        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(issued.rawToken()).isNotBlank();
        assertThat(saved.getTokenHash()).isNotEqualTo(issued.rawToken());
        assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    void rotate_withValidToken_revokesOldAndIssuesNew() {
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("irrelevant-because-lookup-is-mocked")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        RefreshTokenService.IssuedToken rotated = refreshTokenService.rotate("some-raw-token", request);

        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.getRevokedAt()).isNotNull();
        assertThat(rotated.rawToken()).isNotBlank();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class)); // old revoked + new issued
        verify(refreshTokenRepository, never()).revokeAllByUserId(any());
    }

    @Test
    void rotate_withExpiredToken_throwsAndDoesNotIssueNew() {
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hash")
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate("expired-token", request))
                .isInstanceOf(BusinessRuleException.class);

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotate_withUnknownToken_throwsInvalidRefreshToken() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("does-not-exist", request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getErrorCode())
                        .isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void rotate_withAlreadyRevokedToken_revokesAllSessionsAndRejects() {
        RefreshToken reused = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(true)
                .revokedAt(OffsetDateTime.now().minusMinutes(5))
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(reused));

        assertThatThrownBy(() -> refreshTokenService.rotate("stolen-old-token", request))
                .isInstanceOf(BusinessRuleException.class);

        verify(refreshTokenRepository).revokeAllByUserId(eq(user.getId()));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void revoke_marksMatchingTokenRevoked() {
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .tokenHash("hash")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        refreshTokenService.revoke("some-token");

        assertThat(existing.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    void revoke_withUnknownToken_doesNothingSilently() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        refreshTokenService.revoke("unknown-token");

        verify(refreshTokenRepository, never()).save(any());
    }
}
