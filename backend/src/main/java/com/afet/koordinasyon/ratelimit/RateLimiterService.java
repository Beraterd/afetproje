package com.afet.koordinasyon.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.TimeMeter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tek-instance, in-memory rate limiter (Bucket4j). Anahtar = policy adı + kimlik (IP veya userId).
 *
 * ÖNEMLİ (bkz. docs/PRODUCTION_CHECKLIST.md): Bu limiter JVM belleğinde tutulur — birden fazla
 * backend instance'ı (örn. yatay ölçekleme, rolling deploy sırasında iki pod) arkasında çalışan
 * bir deployment'ta her instance kendi sayaçlarını tutar, dolayısıyla gerçek limit instance
 * sayısıyla orantılı şekilde gevşer. Multi-instance deployment için Redis tabanlı distributed
 * bir limiter (örn. Bucket4j'nin Redis entegrasyonu) gerekir.
 */
@Component
@Slf4j
public class RateLimiterService {

    private record Entry(Bucket bucket, RateLimitPolicy policy) {
    }

    private final Map<String, Entry> buckets = new ConcurrentHashMap<>();
    private final TimeMeter timeMeter;

    public RateLimiterService() {
        this(TimeMeter.SYSTEM_MILLISECONDS);
    }

    /** Testlerin gerçek dakikalarca beklemeden pencere/refill davranışını doğrulayabilmesi için. */
    RateLimiterService(TimeMeter timeMeter) {
        this.timeMeter = timeMeter;
    }

    public boolean tryConsume(RateLimitPolicy policy, String identity) {
        String key = policy.name() + ":" + identity;
        Entry entry = buckets.computeIfAbsent(key, k -> new Entry(newBucket(policy), policy));
        return entry.bucket().tryConsume(1);
    }

    private Bucket newBucket(RateLimitPolicy policy) {
        Bandwidth limit = Bandwidth.classic(policy.capacity(),
                Refill.greedy(policy.capacity(), policy.window()));
        return Bucket.builder().withCustomTimePrecision(timeMeter).addLimit(limit).build();
    }

    /**
     * Kullanılmayan bucket'ları belleten temizler — tamamen dolu (son pencerede hiç istek
     * tüketilmemiş) bucket'lar kabaca "durgun" kabul edilir. Bellek büyümesini sınırlamak
     * içindir; yanlışlıkla aktif bir bucket'ı sıfırlarsa (nadiren) sonucu sadece limitin
     * o an için tazelenmesidir, güvenlik açığı doğurmaz.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void evictIdleBuckets() {
        int before = buckets.size();
        buckets.entrySet().removeIf(e -> e.getValue().bucket().getAvailableTokens() >= e.getValue().policy().capacity());
        int removed = before - buckets.size();
        if (removed > 0) {
            log.debug("Rate limiter: {} durgun bucket temizlendi ({} kaldı)", removed, buckets.size());
        }
    }
}
