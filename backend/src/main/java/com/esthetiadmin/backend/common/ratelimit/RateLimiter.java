package com.esthetiadmin.backend.common.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts hits per key (an IP) in a fixed window and says when the key ran out (ADR 015). In memory: there is a
 * single backend instance and losing the counts on a restart is acceptable.
 */
public class RateLimiter {

    /** Above this many keys the expired windows are swept, so the map cannot grow without limit. */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final int maxHits;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int maxHits, Duration window, Clock clock) {
        this.maxHits = maxHits;
        this.window = window;
        this.clock = clock;
    }

    /** Counts a hit and says whether it is still allowed (the hit that goes over the limit is not). */
    public boolean tryAcquire(String key) {
        return record(key) <= maxHits;
    }

    /** Whether the key already used up its window, without counting anything. */
    public boolean isExhausted(String key) {
        Window current = windows.get(key);
        return current != null && !current.expired(clock.instant()) && current.hits >= maxHits;
    }

    /** Counts a hit (for example, a failed login) and returns how many the key has in its window. */
    public int record(String key) {
        Instant now = clock.instant();
        sweepIfCrowded(now);
        Window updated = windows.compute(key, (k, current) ->
                current == null || current.expired(now) ? new Window(now.plus(window), 1) : current.next());
        return updated.hits;
    }

    private void sweepIfCrowded(Instant now) {
        if (windows.size() > SWEEP_THRESHOLD) {
            windows.values().removeIf(current -> current.expired(now));
        }
    }

    private record Window(Instant endsAt, int hits) {

        boolean expired(Instant now) {
            return !now.isBefore(endsAt);
        }

        Window next() {
            return new Window(endsAt, hits + 1);
        }
    }
}
