package com.afet.koordinasyon.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * "Admin Demo Modu" için son çare güvenlik katmanı: demo hesabıyla kimliği doğrulanmış her
 * istekte, HTTP metodu değişiklik yapan bir metotsa (POST/PUT/PATCH/DELETE) isteği controller'a
 * ULAŞTIRMADAN 403 ile keser. Kayıt kaydetmeyen AI önizlemeleri ve parola ile oturum değiştirme/kapatma
 * aşağıdaki dar izin listesine tabidir. Bu davranış, hangi endpoint'in @PreAuthorize kullandığına
 * bakılmaksızın geçerlidir — yeni bir yazma endpoint'i eklendiğinde ekstra bir şey yapılmasına
 * gerek yoktur, tek istisna elle eklenmiş bir @PreAuthorize değil, bu filtredir.
 *
 * JwtAuthenticationFilter'dan SONRA çalışacak şekilde SecurityConfig'de kaydedilir; böylece
 * SecurityContextHolder içindeki principal (varsa) zaten doludur.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoModeWriteGuardFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of(
            HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name());

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (MUTATING_METHODS.contains(request.getMethod()) && isDemoPrincipal() && !isAllowedDemoRequest(request)) {
            writeRestrictedResponse(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isDemoPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof UserPrincipal principal
                && principal.isDemo();
    }

    private boolean isAllowedDemoRequest(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) return false;
        String path = request.getRequestURI();
        return path.equals("/api/ai/operations-assistant")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/logout")
                || path.equals("/api/team-recommendations")
                || path.matches("/api/damage-assessments/[0-9a-fA-F-]{36}/ai-preview")
                || path.matches("/api/events/[0-9a-fA-F-]{36}/team-recommendations");
    }

    private void writeRestrictedResponse(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        log.debug("Demo modunda yazma isteği engellendi: {} {}", request.getMethod(), request.getRequestURI());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", HttpServletResponse.SC_FORBIDDEN);
        body.put("error", DemoModeConstants.RESTRICTED_ERROR_CODE);
        body.put("message", DemoModeConstants.RESTRICTED_MESSAGE);
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("path", request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
