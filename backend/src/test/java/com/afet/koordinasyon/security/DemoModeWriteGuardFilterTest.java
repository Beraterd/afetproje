package com.afet.koordinasyon.security;

import com.afet.koordinasyon.domain.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Demo modundaki (salt okunur) bir kullanıcının yazma isteklerinin (POST/PUT/PATCH/DELETE)
 * controller'a ulaşmadan bu filtre tarafından reddedildiğini doğrular.
 */
class DemoModeWriteGuardFilterTest {

    private final DemoModeWriteGuardFilter filter = new DemoModeWriteGuardFilter(new ObjectMapper());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserPrincipal demoPrincipal() {
        return new UserPrincipal(UUID.randomUUID(), "Demo", "Admin", "demo.admin@demo.local",
                null, UserRole.ADMIN, null, null, true, true, List.of());
    }

    private UserPrincipal realPrincipal() {
        return new UserPrincipal(UUID.randomUUID(), "Gerçek", "Admin", "admin@x.com",
                null, UserRole.ADMIN, null, null, true, false, List.of());
    }

    @Test
    @DisplayName("Demo hesabıyla yapılan POST isteği 403 ile engellenir ve controller'a ulaşmaz")
    void demoUser_postRequest_isRejected() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(demoPrincipal(), null, List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentAsString()).contains("DEMO_MODE_RESTRICTED");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Demo hesabıyla yapılan GET isteği engellenmez")
    void demoUser_getRequest_passesThrough() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(demoPrincipal(), null, List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Gerçek (demo olmayan) kullanıcının POST isteği engellenmez")
    void realUser_postRequest_passesThrough() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(realPrincipal(), null, List.of()));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/events");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
    }
}
