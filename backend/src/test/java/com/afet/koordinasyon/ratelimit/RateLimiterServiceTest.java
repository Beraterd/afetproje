package com.afet.koordinasyon.ratelimit;

import io.github.bucket4j.TimeMeter;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Bucket4j tabanlı rate limiter'ı gerçek dakikalarca beklemeden doğrular — sahte bir
 * {@link TimeMeter} ile zamanı elle ilerletiyoruz (item 45 test gereksinimi: "gerçek dakika
 * beklemeden clock abstraction kullan").
 */
class RateLimiterServiceTest {

    private static class FakeTimeMeter implements TimeMeter {
        private long nanos = 0L;

        @Override
        public long currentTimeNanos() {
            return nanos;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }

        void advance(Duration duration) {
            nanos += duration.toNanos();
        }
    }

    @Test
    void tryConsume_allowsUpToCapacity_thenBlocks() {
        FakeTimeMeter clock = new FakeTimeMeter();
        RateLimiterService service = new RateLimiterService(clock);
        String identity = "ip:1.2.3.4";

        for (int i = 0; i < RateLimitPolicy.LOGIN.capacity(); i++) {
            assertThat(service.tryConsume(RateLimitPolicy.LOGIN, identity)).isTrue();
        }
        assertThat(service.tryConsume(RateLimitPolicy.LOGIN, identity)).isFalse();
    }

    @Test
    void tryConsume_refillsAfterWindow_withoutRealWaiting() {
        FakeTimeMeter clock = new FakeTimeMeter();
        RateLimiterService service = new RateLimiterService(clock);
        String identity = "ip:5.6.7.8";

        for (int i = 0; i < RateLimitPolicy.LOGIN.capacity(); i++) {
            service.tryConsume(RateLimitPolicy.LOGIN, identity);
        }
        assertThat(service.tryConsume(RateLimitPolicy.LOGIN, identity)).isFalse();

        clock.advance(RateLimitPolicy.LOGIN.window().plusSeconds(1));

        assertThat(service.tryConsume(RateLimitPolicy.LOGIN, identity)).isTrue();
    }

    @Test
    void tryConsume_differentIdentities_areIndependent() {
        FakeTimeMeter clock = new FakeTimeMeter();
        RateLimiterService service = new RateLimiterService(clock);

        for (int i = 0; i < RateLimitPolicy.LOGIN.capacity(); i++) {
            service.tryConsume(RateLimitPolicy.LOGIN, "ip:A");
        }
        assertThat(service.tryConsume(RateLimitPolicy.LOGIN, "ip:A")).isFalse();
        assertThat(service.tryConsume(RateLimitPolicy.LOGIN, "ip:B")).isTrue();
    }
}
