package com.blikeng.chess.unit.security;

import com.blikeng.chess.security.ratelimit.RateLimitingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingServiceTest {

    RateLimitingService rateLimitingService;

    @BeforeEach
    void setup() {
        rateLimitingService = new RateLimitingService();
    }

    @Test
    void shouldAllowRequestsWithinLimit() {
        assertThat(rateLimitingService.tryConsume("key", 5L, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void shouldBlockRequestsExceedingLimit() {
        for (int i = 0; i < 3; i++) {
            rateLimitingService.tryConsume("key", 3L, Duration.ofMinutes(1));
        }

        assertThat(rateLimitingService.tryConsume("key", 3L, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void shouldTrackDifferentKeysSeparately() {
        for (int i = 0; i < 3; i++) {
            rateLimitingService.tryConsume("key1", 3L, Duration.ofMinutes(1));
        }

        assertThat(rateLimitingService.tryConsume("key2", 3L, Duration.ofMinutes(1))).isTrue();
    }
}