package com.afet.koordinasyon.ratelimit;

import com.afet.koordinasyon.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Risk bazlı endpoint'ler için Bucket4j tabanlı rate limiting (item 45).
 * JwtAuthenticationFilter'dan SONRA çalışır (SecurityContext'te varsa authenticated principal
 * hazır olsun diye — SEARCH/AI/UPLOAD kullanıcı bazlı limitlenir).
 *
 * Sınırı aşan istek 429 + Retry-After header'ı ile DemoModeWriteGuardFilter'la aynı üslupta
 * (ErrorResponse şekline benzer, ama bu bir servlet filter olduğu için GlobalExceptionHandler'a
 * uğramaz — JSON gövdeyi burada elle yazıyoruz) Türkçe mesajla kesilir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private record Rule(HttpMethod method, PathPattern pattern, RateLimitPolicy policy, boolean userScoped) {
    }

    private static final PathPatternParser PARSER = new PathPatternParser();

    private final List<Rule> rules = List.of(
            new Rule(HttpMethod.POST, PARSER.parse("/api/auth/login"), RateLimitPolicy.LOGIN, false),
            new Rule(HttpMethod.POST, PARSER.parse("/api/auth/demo-login"), RateLimitPolicy.LOGIN, false),
            new Rule(HttpMethod.POST, PARSER.parse("/api/auth/refresh"), RateLimitPolicy.REFRESH, false),
            new Rule(HttpMethod.GET, PARSER.parse("/api/search"), RateLimitPolicy.SEARCH, true),
            new Rule(HttpMethod.POST, PARSER.parse("/api/ai/operations-assistant"), RateLimitPolicy.AI, true),
            new Rule(HttpMethod.POST, PARSER.parse("/api/damage-assessments/{id}/ai-analysis"), RateLimitPolicy.AI, true),
            new Rule(HttpMethod.POST, PARSER.parse("/api/damage-assessments/ai/enqueue-missing"), RateLimitPolicy.AI, true),
            new Rule(HttpMethod.POST, PARSER.parse("/api/users/me/documents"), RateLimitPolicy.UPLOAD, true),
            new Rule(HttpMethod.POST, PARSER.parse("/api/damage-assessments"), RateLimitPolicy.UPLOAD, true),
            new Rule(HttpMethod.POST, PARSER.parse("/api/my-tasks/damage-assessments/{assignmentId}/photos"), RateLimitPolicy.UPLOAD, true)
    );

    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Rule matched = findRule(request);
        if (matched != null) {
            String identity = matched.userScoped() ? resolveUserOrIp(request) : clientIpResolver.resolve(request);
            boolean allowed = rateLimiterService.tryConsume(matched.policy(), identity);
            if (!allowed) {
                log.warn("Rate limit aşıldı: policy={} identity={} path={}", matched.policy(), identity,
                        request.getRequestURI());
                writeRateLimitedResponse(request, response, matched.policy());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private Rule findRule(HttpServletRequest request) {
        String method = request.getMethod();
        for (Rule rule : rules) {
            if (rule.method().matches(method) && rule.pattern().matches(
                    org.springframework.http.server.PathContainer.parsePath(request.getRequestURI()))) {
                return rule;
            }
        }
        return null;
    }

    private String resolveUserOrIp(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + principal.getId();
        }
        return "ip:" + clientIpResolver.resolve(request);
    }

    private void writeRateLimitedResponse(HttpServletRequest request, HttpServletResponse response,
            RateLimitPolicy policy) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", 429);
        body.put("error", "RATE_LIMITED");
        body.put("message", "Çok fazla deneme yapıldı. Lütfen kısa süre sonra tekrar deneyin.");
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("path", request.getRequestURI());
        body.put("requestId", MDC.get("requestId"));

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(policy.window().toSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
