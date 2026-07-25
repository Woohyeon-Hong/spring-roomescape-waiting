package roomescape.rateLimit;

import java.util.function.LongSupplier;

public class TokenBucketRateLimiter {

    private final long capacity;
    private final double refillPerSec;
    private final LongSupplier nanoClock;

    private double availableTokens;
    private long lastRefillNanos;

    public TokenBucketRateLimiter(long capacity, double refillPerSec, LongSupplier nanoClock) {
        this.capacity = capacity;
        this.refillPerSec = refillPerSec;
        this.nanoClock = nanoClock;
        this.availableTokens = capacity;
        this.lastRefillNanos = nanoClock.getAsLong();
    }

    public synchronized boolean tryConsume() {
        refill();

        if (availableTokens < 1) {
            return false;
        }

        availableTokens -= 1;
        return true;
    }

    private void refill() {
        long now = nanoClock.getAsLong();
        double elapsedSec = (now - lastRefillNanos) / 1_000_000_000.0;
        double refiled = elapsedSec * refillPerSec;

        availableTokens = availableTokens + refiled;

        if (availableTokens > capacity) {
            availableTokens = capacity;
        }

        lastRefillNanos = now;
    }

    public synchronized long retryAfterSeconds() {
        refill();

        if (availableTokens >= 1) {
            return 0;
        }

        return (long) Math.ceil((1 - availableTokens) / refillPerSec);
    }
}
