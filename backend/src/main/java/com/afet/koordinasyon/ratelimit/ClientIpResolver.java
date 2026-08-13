package com.afet.koordinasyon.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Gerçek client IP'sini çözer. `X-Forwarded-For` header'ına yalnızca istek doğrudan
 * güvenilen bir reverse proxy'den (`app.security.trusted-proxies`) geliyorsa güvenilir —
 * aksi halde spoof edilebileceği için doğrudan `request.getRemoteAddr()` kullanılır.
 *
 * Nginx/reverse proxy arkasında çalışan bir deployment'ta `app.security.trusted-proxies`
 * proxy'nin gerçek bağlantı IP'sine (örn. 127.0.0.1) ayarlanmalıdır.
 */
@Component
public class ClientIpResolver {

    private final Set<String> trustedProxies;

    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String trustedProxiesCsv) {
        this.trustedProxies = StringUtils.hasText(trustedProxiesCsv)
                ? Set.copyOf(List.of(trustedProxiesCsv.split(",")).stream().map(String::trim).toList())
                : Set.of();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxies.contains(remoteAddr)) {
            return remoteAddr;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return remoteAddr;
    }
}
