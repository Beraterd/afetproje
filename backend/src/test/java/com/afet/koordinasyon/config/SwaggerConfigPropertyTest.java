package com.afet.koordinasyon.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Springdoc/Swagger'ın production'da kapalı, development/default profilde açık olduğunu
 * doğrular. Gerçek uygulama context'ini (DB, güvenlik filtreleri vb.) hiç yüklemeden — yalnızca
 * application.yml + application-prod.yml'in ConfigData olarak çözümlenen property değerlerini
 * okur. Bu ortamda PostgreSQL/Docker olmadığı için ağır bir @SpringBootTest tercih edilmedi.
 */
class SwaggerConfigPropertyTest {

    @Configuration
    static class EmptyConfig {
    }

    private ConfigurableApplicationContext contextForProfile(String profile) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(EmptyConfig.class)
                .web(WebApplicationType.NONE);
        if (profile != null) {
            builder.profiles(profile);
        }
        return builder.run();
    }

    @Test
    void prodProfile_disablesSwagger() {
        try (ConfigurableApplicationContext context = contextForProfile("prod")) {
            assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class))
                    .isFalse();
            assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class))
                    .isFalse();
        }
    }

    @Test
    void defaultProfile_leavesSwaggerEnabled() {
        try (ConfigurableApplicationContext context = contextForProfile(null)) {
            // application.yml springdoc.api-docs.enabled / swagger-ui.enabled hiç set etmiyor —
            // springdoc'un kendi varsayılanı `true`'dur, yani key hiç override edilmemiş demektir.
            assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled")).isNull();
            assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled")).isNull();
            assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.path"))
                    .isEqualTo("/swagger-ui.html");
        }
    }
}
