package com.esthetiadmin.backend.common.ratelimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/** The limits of ADR 015, one limiter per use. */
@Configuration
public class RateLimitConfig {

    public static final String PUBLIC_BOOKING = "publicBookingLimiter";
    public static final String FAILED_LOGIN = "failedLoginLimiter";

    @Bean(PUBLIC_BOOKING)
    RateLimiter publicBookingLimiter(Clock clock) {
        return new RateLimiter(5, Duration.ofHours(1), clock);
    }

    @Bean(FAILED_LOGIN)
    RateLimiter failedLoginLimiter(Clock clock) {
        return new RateLimiter(5, Duration.ofMinutes(15), clock);
    }
}
