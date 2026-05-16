package com.roofingcrm.security.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinuteWindowRateLimiterTest {

    @Test
    void allowsUpToMaxPerWindow_thenBlocks() {
        MinuteWindowRateLimiter limiter = new MinuteWindowRateLimiter();
        String key = "scope-a";
        assertTrue(limiter.tryAcquire(key, 3));
        assertTrue(limiter.tryAcquire(key, 3));
        assertTrue(limiter.tryAcquire(key, 3));
        assertFalse(limiter.tryAcquire(key, 3));
    }

    @Test
    void separateKeysAreIndependent() {
        MinuteWindowRateLimiter limiter = new MinuteWindowRateLimiter();
        assertTrue(limiter.tryAcquire("login:a", 1));
        assertFalse(limiter.tryAcquire("login:a", 1));
        assertTrue(limiter.tryAcquire("login:b", 1));
    }

    @Test
    void prunesStaleKeysAfterClockAdvancesTwoMinutes() {
        AtomicLong clock = new AtomicLong(0L);
        MinuteWindowRateLimiter limiter = new MinuteWindowRateLimiter(clock::get);
        for (int i = 0; i < 5000; i++) {
            assertTrue(limiter.tryAcquire("k" + i, 5));
        }
        assertEquals(5000, limiter.testingEntryCount());
        clock.set(120_000L);
        assertTrue(limiter.tryAcquire("fresh", 5));
        assertEquals(1, limiter.testingEntryCount());
    }

    @Test
    void retainsKeysFromPreviousMinuteWindow() {
        AtomicLong clock = new AtomicLong(0L);
        MinuteWindowRateLimiter limiter = new MinuteWindowRateLimiter(clock::get);
        assertTrue(limiter.tryAcquire("held", 5));
        clock.set(60_000L);
        assertTrue(limiter.tryAcquire("other", 5));
        assertEquals(2, limiter.testingEntryCount());
    }
}
