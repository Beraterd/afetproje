package com.afet.koordinasyon.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Item 48 — prod profili aktifken kritik yanlış yapılandırmaların startup'ı FAIL-FAST
 * durdurduğunu, dev/local'i etkilemediğini doğrular.
 */
class ProductionReadinessValidatorTest {

    private static final String SECURE_JWT_SECRET = "a-real-production-secret-that-is-at-least-32-chars-long";

    private UrlBasedCorsConfigurationSource validCorsSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://afetistanbul.online"));
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private ProductionReadinessValidator build(String[] activeProfiles, String jwtSecret,
            boolean refreshCookieSecure, boolean apiDocsEnabled, boolean swaggerUiEnabled,
            UrlBasedCorsConfigurationSource corsSource) {
        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(activeProfiles);

        ProductionReadinessValidator validator = new ProductionReadinessValidator(environment, corsSource);
        ReflectionTestUtils.setField(validator, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(validator, "refreshCookieSecure", refreshCookieSecure);
        ReflectionTestUtils.setField(validator, "apiDocsEnabled", apiDocsEnabled);
        ReflectionTestUtils.setField(validator, "swaggerUiEnabled", swaggerUiEnabled);
        return validator;
    }

    @Test
    void prodProfile_withInsecureRefreshCookie_failsFast() {
        var validator = build(new String[] { "prod" }, SECURE_JWT_SECRET,
                /*refreshCookieSecure*/ false, false, false, validCorsSource());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("refresh-cookie-secure");
    }

    @Test
    void prodProfile_withDefaultJwtSecret_failsFast() {
        var validator = build(new String[] { "prod" },
                "afet-koordinasyon-super-secret-jwt-key-minimum-256-bits-long-2026",
                true, false, false, validCorsSource());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret");
    }

    @Test
    void prodProfile_withSwaggerEnabled_failsFast() {
        var validator = build(new String[] { "prod" }, SECURE_JWT_SECRET, true,
                /*apiDocsEnabled*/ true, false, validCorsSource());

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Swagger");
    }

    @Test
    void prodProfile_withWildcardCors_failsFast() {
        UrlBasedCorsConfigurationSource wildcardSource = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        wildcardSource.registerCorsConfiguration("/**", config);

        var validator = build(new String[] { "prod" }, SECURE_JWT_SECRET, true, false, false, wildcardSource);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CORS");
    }

    @Test
    void prodProfile_withValidConfig_startsNormally() {
        var validator = build(new String[] { "prod" }, SECURE_JWT_SECRET, true, false, false, validCorsSource());

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }

    @Test
    void devProfile_withInsecureConfig_isNotValidated() {
        // prod olmayan profillerde (dev/local/default) hiçbir kontrol çalışmaz — mevcut
        // geliştirme akışı bozulmaz.
        var validator = build(new String[] { "default" },
                "afet-koordinasyon-super-secret-jwt-key-minimum-256-bits-long-2026",
                false, true, true, validCorsSource());

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
