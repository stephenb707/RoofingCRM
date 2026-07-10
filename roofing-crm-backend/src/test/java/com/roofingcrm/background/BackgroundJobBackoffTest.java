package com.roofingcrm.background;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BackgroundJobBackoffTest {

    @Test
    void delaysFollowSimpleStaircase() {
        assertEquals(Duration.ZERO, BackgroundJobBackoff.delayAfterFailureNumber(0));
        assertEquals(Duration.ofMinutes(1), BackgroundJobBackoff.delayAfterFailureNumber(1));
        assertEquals(Duration.ofMinutes(5), BackgroundJobBackoff.delayAfterFailureNumber(2));
        assertEquals(Duration.ofMinutes(15), BackgroundJobBackoff.delayAfterFailureNumber(3));
        assertEquals(Duration.ofHours(1), BackgroundJobBackoff.delayAfterFailureNumber(4));
        assertEquals(Duration.ofHours(1), BackgroundJobBackoff.delayAfterFailureNumber(99));
    }
}
