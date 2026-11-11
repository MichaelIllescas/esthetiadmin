package com.esthetiadmin.backend.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    private static final Instant START = Instant.parse("2026-10-03T12:00:00Z");

    /** A clock that the test moves forward by hand. */
    private static final class MovableClock extends Clock {
        private Instant now = START;

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final MovableClock clock = new MovableClock();
    private final RateLimiter limiter = new RateLimiter(3, Duration.ofHours(1), clock);

    @Test
    void allowsUpToTheLimitPerKey() {
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void theWindowStartsOverOnceItEnds() {
        for (int hit = 0; hit < 4; hit++) {
            limiter.tryAcquire("a");
        }
        assertThat(limiter.isExhausted("a")).isTrue();

        clock.advance(Duration.ofMinutes(59));
        assertThat(limiter.tryAcquire("a")).isFalse();

        clock.advance(Duration.ofMinutes(1));
        assertThat(limiter.isExhausted("a")).isFalse();
        assertThat(limiter.tryAcquire("a")).isTrue();
    }

    @Test
    void recordingCountsWithoutBlockingAndExhaustionOnlyLooks() {
        assertThat(limiter.isExhausted("a")).isFalse();
        assertThat(limiter.record("a")).isEqualTo(1);
        assertThat(limiter.record("a")).isEqualTo(2);
        assertThat(limiter.isExhausted("a")).isFalse();
        assertThat(limiter.record("a")).isEqualTo(3);
        assertThat(limiter.isExhausted("a")).isTrue();
    }
}
