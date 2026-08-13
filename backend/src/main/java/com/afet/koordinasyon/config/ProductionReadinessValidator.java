package com.afet.koordinasyon.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Item 48 — production preflight: {@code prod} profili aktifken kritik güvenlik configlerini
 * doğrular. Yanlış yapılandırılmış bir production instance'ının SESSİZCE açılmasını önlemek
 * için, gerçekten kritik bir yanlış yapılandırma bulunursa {@link #afterPropertiesSet}
 * (bean initialization sırasında, embedded servlet container henüz dinlemeye BAŞLAMADAN önce)
 * fail-fast şekilde exception fırlatır — uygulama hiç ayağa kalkmaz.
 *
 * dev/local (prod profili aktif değilken) hiçbir kontrol çalışmaz, mevcut geliştirme akışı
 * etkilenmez.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionReadinessValidator {

    private static final String DEFAULT_JWT_SECRET =
            "afet-koordinasyon-super-secret-jwt-key-minimum-256-bits-long-2026";
    private static final int MIN_JWT_SECRET_LENGTH = 32; // 256 bit

    private final Environment environment;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @Value("${app.security.refresh-cookie-secure:false}")
    private boolean refreshCookieSecure;

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean apiDocsEnabled;

    @Value("${springdoc.swagger-ui.enabled:true}")
    private boolean swaggerUiEnabled;

    @PostConstruct
    public void validate() {
        if (!isProdProfileActive()) {
            return;
        }

        List<String> failures = new ArrayList<>();

        if (!StringUtils.hasText(jwtSecret) || DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            failures.add("app.jwt.secret varsayılan/boş değerde — JWT_SECRET env değişkeni gerçek bir gizli değerle set edilmeli.");
        } else if (jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            failures.add("app.jwt.secret çok kısa (" + jwtSecret.length()
                    + " karakter) — en az " + MIN_JWT_SECRET_LENGTH + " karakter (256 bit) olmalı.");
        }

        if (!refreshCookieSecure) {
            failures.add("app.security.refresh-cookie-secure=false — production'da HTTPS üzerinden Secure cookie zorunlu, "
                    + "JWT_REFRESH_COOKIE_SECURE=true set edilmeli.");
        }

        if (apiDocsEnabled || swaggerUiEnabled) {
            failures.add("Swagger/OpenAPI production'da açık kalmış (springdoc.api-docs.enabled / swagger-ui.enabled) — "
                    + "kapatılmalı.");
        }

        String wildcardOrigin = findWildcardCorsOrigin();
        if (wildcardOrigin != null) {
            failures.add("CORS allowed-origins wildcard içeriyor (" + wildcardOrigin
                    + ") — production'da yalnızca explicit origin'lere izin verilmeli.");
        }

        if (!failures.isEmpty()) {
            String report = String.join("\n  - ", failures);
            log.error("PRODUCTION STARTUP VALIDATION FAILED — uygulama başlatılmıyor:\n  - {}", report);
            throw new IllegalStateException(
                    "Production konfigürasyonu güvensiz, başlatma durduruldu:\n  - " + report);
        }

        log.info("Production startup validation: tüm kritik güvenlik configleri geçerli.");
    }

    private boolean isProdProfileActive() {
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private String findWildcardCorsOrigin() {
        if (!(corsConfigurationSource instanceof UrlBasedCorsConfigurationSource urlBased)) {
            return null;
        }
        for (CorsConfiguration config : urlBased.getCorsConfigurations().values()) {
            if (config.getAllowedOrigins() != null && config.getAllowedOrigins().contains("*")) {
                return "*";
            }
            if (config.getAllowedOriginPatterns() != null && config.getAllowedOriginPatterns().contains("*")) {
                return "*";
            }
        }
        return null;
    }
}
