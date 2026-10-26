package com.example.crud.config;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginRateLimiterTest {
    @Test
    void blocksAtLimitAndResetsAtExactWindowBoundary() {
        var now = new AtomicLong();
        var limiter = new LoginRateLimiter(2, 10, Duration.ofSeconds(60), now::get);
        assertTrue(limiter.acquire("one").allowed());
        assertTrue(limiter.acquire("one").allowed());
        assertEquals(60, limiter.acquire("one").retryAfterSeconds());
        now.set(Duration.ofMillis(59_001).toNanos());
        assertEquals(1, limiter.acquire("one").retryAfterSeconds());
        assertTrue(limiter.acquire("two").allowed());
        now.set(Duration.ofSeconds(60).toNanos());
        assertTrue(limiter.acquire("one").allowed());
    }

    @Test
    void capacityFailsClosedAndExpiredSlotsCanBeReused() {
        var now = new AtomicLong();
        var limiter = new LoginRateLimiter(1, 2, Duration.ofSeconds(60), now::get);
        assertTrue(limiter.acquire("one").allowed());
        assertTrue(limiter.acquire("two").allowed());
        assertFalse(limiter.acquire("three").allowed());
        assertFalse(limiter.acquire("one").allowed());
        now.set(Duration.ofSeconds(60).toNanos());
        assertTrue(limiter.acquire("three").allowed());
    }

    @Test
    void concurrentRequestsCannotExceedTheLimit() {
        var limiter = new LoginRateLimiter(10, 2, Duration.ofSeconds(60), () -> 0);
        long allowed = IntStream.range(0, 1000).parallel()
                .filter(i -> limiter.acquire("same").allowed()).count();
        assertEquals(10, allowed);
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThrows(IllegalArgumentException.class,
                () -> new LoginRateLimiter(0, 1, Duration.ofSeconds(1), () -> 0));
        assertThrows(IllegalArgumentException.class,
                () -> new LoginRateLimiter(1, 0, Duration.ofSeconds(1), () -> 0));
        assertThrows(IllegalArgumentException.class,
                () -> new LoginRateLimiter(1, 1, Duration.ZERO, () -> 0));
        assertThrows(IllegalArgumentException.class,
                () -> new LoginRateLimiter(1, 1, Duration.ofDays(2), () -> 0));
    }
}
