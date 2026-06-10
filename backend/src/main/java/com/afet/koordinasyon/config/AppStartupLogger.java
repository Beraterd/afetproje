package com.afet.koordinasyon.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
@Slf4j
public class AppStartupLogger {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.short-link.base-url}")
    private String shortLinkBaseUrl;

    private final Environment environment;

    public AppStartupLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String[] activeProfiles = environment.getActiveProfiles();
        String profiles = activeProfiles.length > 0
                ? Arrays.toString(activeProfiles)
                : "(default)";

        log.info("=== AFET Koordinasyon başlatıldı ===");
        log.info("Active Profile:       {}", profiles);
        log.info("Frontend URL:         {}", frontendUrl);
        log.info("Backend Base URL:     {}", baseUrl);
        log.info("Short Link Base URL:  {}", shortLinkBaseUrl);
        log.info("====================================");
    }
}
