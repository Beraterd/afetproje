package com.afet.koordinasyon.security;

import com.afet.koordinasyon.ratelimit.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final DemoModeWriteGuardFilter demoModeWriteGuardFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(headers -> headers
            .contentTypeOptions(withDefaults())
            .frameOptions(frame -> frame.deny())
            .referrerPolicy(referrer -> referrer.policy(
                ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
            // Backend saf JSON API — hiçbir controller HTML döndürmüyor (Swagger UI hariç, o da
            // prod'da springdoc.enabled=false ile zaten kapalı). Bu yüzden default-src 'none'
            // güvenle uygulanabilir; Swagger'ın kendi HTML/JS/CSS'ini local/dev'de çalıştırabilmesi
            // için bu path'ler CSP'den muaf tutulur.
            .addHeaderWriter(cspHeaderWriter())
        )
        .authorizeHttpRequests(auth -> auth
            // Public endpoints — deliberately enumerated one by one, no broad wildcards.
            .requestMatchers(
                "/api/auth/login",
                "/api/auth/demo-login",
                "/api/auth/register",
                "/api/auth/refresh",
                "/api/auth/logout",
                "/api/auth/forgot-password",
                "/api/auth/reset-password",
                "/api/auth/reset-password/validate",
                "/s/**",
                "/api/dashboard/health",
                "/api/districts/**",
                "/api/files/**",
                "/api/event-assignments/accept",
                "/api/event-assignments/decline",
                "/api/emergency-message/**",
                "/api/emergency/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api-docs/**",
                "/v3/api-docs/**",
                "/actuator/health",
                "/actuator/health/**"
            ).permitAll()
            // Static team type labels — no personal or operational data.
            .requestMatchers(HttpMethod.GET, "/api/teams/types").permitAll()

            // Actuator: only /actuator/health is public (matched above); everything else
            // (metrics, info, env, etc.) requires an authenticated ADMIN.
            .requestMatchers("/actuator/**").hasRole("ADMIN")

            // Everything else (including /api/auth/me and the rest of /api/teams/**)
            // requires authentication; finer-grained role checks live on @PreAuthorize.
            .anyRequest().authenticated()
        );

    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class);
    http.addFilterAfter(demoModeWriteGuardFilter, RateLimitFilter.class);
    return http.build();
}
    private DelegatingRequestMatcherHeaderWriter cspHeaderWriter() {
        RequestMatcher swaggerPaths = new OrRequestMatcher(
                new AntPathRequestMatcher("/swagger-ui/**"),
                new AntPathRequestMatcher("/swagger-ui.html"),
                new AntPathRequestMatcher("/api-docs/**"),
                new AntPathRequestMatcher("/v3/api-docs/**")
        );
        return new DelegatingRequestMatcherHeaderWriter(
                new NegatedRequestMatcher(swaggerPaths),
                new ContentSecurityPolicyHeaderWriter("default-src 'none'; frame-ancestors 'none'"));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000", "http://localhost:3001", "http://localhost:5173",
                "https://afetistanbul.online", "https://www.afetistanbul.online"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
