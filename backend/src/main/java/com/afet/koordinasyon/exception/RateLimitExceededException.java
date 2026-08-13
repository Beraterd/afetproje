package com.afet.koordinasyon.exception;

/**
 * Bir istemci risk bazlı rate limit penceresini aştığında fırlatılır (login, search, AI, vb).
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
