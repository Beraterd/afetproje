package com.afet.koordinasyon.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * AI (Claude) sağlayıcısı OPSİYONEL bir bağımlılıktır — sistemin çekirdek işlevi (afet
 * koordinasyonu, kaynak yönetimi, hasar tespiti) AI olmadan da tam çalışır. Bu yüzden bu
 * indicator readiness/liveness gruplarına DAHİL EDİLMEZ (bkz. application.yml
 * management.endpoint.health.group.readiness.include) ve gerçek bir API çağrısı yapmaz
 * (maliyetli + rate limit'e tabi) — yalnızca yapılandırma durumunu raporlar, hiçbir zaman
 * DOWN döndürmez ki yanlışlıkla genel /actuator/health durumunu aşağı çekmesin.
 */
@Component("aiProvider")
public class AiProviderHealthIndicator implements HealthIndicator {

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.claude-api-key:}")
    private String apiKey;

    @Override
    public Health health() {
        boolean configured = aiEnabled && StringUtils.hasText(apiKey);
        return Health.up()
                .withDetail("enabled", aiEnabled)
                .withDetail("configured", configured)
                .build();
    }
}
