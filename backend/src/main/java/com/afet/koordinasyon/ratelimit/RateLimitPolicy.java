package com.afet.koordinasyon.ratelimit;

import java.time.Duration;

/**
 * Risk bazlı rate limit politikaları. Tek-instance deployment için memory-based
 * (Bucket4j in-memory Bucket) — bkz. RateLimiterService ve docs/PRODUCTION_CHECKLIST.md
 * ("multi-instance deployment için distributed rate limiting gerekir").
 */
public enum RateLimitPolicy {

    /**
     * Login/demo-login denemesi — IP başına. Body içeriğini (e-posta) filtre seviyesinde
     * okumak request stream'ini tüketip controller'ın @RequestBody parse'ını bozacağından
     * (ContentCachingRequestWrapper gerektirir) bilinçli olarak yalnızca IP bazlı tutuldu;
     * BCrypt (cost=12) zaten her denemeyi ağırlaştırıyor, bu limit brute-force hızını
     * ayrıca sınırlar.
     */
    LOGIN(10, Duration.ofMinutes(1)),
    /** Refresh token exchange — meşru sessiz yenileme akışları için daha yüksek limit. */
    REFRESH(30, Duration.ofMinutes(1)),
    /** Global search — kullanıcı başına. */
    SEARCH(30, Duration.ofMinutes(1)),
    /** AI destekli uçlar (operations-assistant, hasar AI analizi) — maliyetli, düşük limit. */
    AI(10, Duration.ofMinutes(5)),
    /** Dosya/fotoğraf upload uçları — kullanıcı başına. */
    UPLOAD(20, Duration.ofMinutes(1));

    private final int capacity;
    private final Duration window;

    RateLimitPolicy(int capacity, Duration window) {
        this.capacity = capacity;
        this.window = window;
    }

    public int capacity() {
        return capacity;
    }

    public Duration window() {
        return window;
    }
}
