package com.roofingcrm.background;

import java.time.Duration;

/**
 * Retry spacing after a failed attempt: 1m, 5m, 15m, then hourly for further tries.
 */
public final class BackgroundJobBackoff {

    private BackgroundJobBackoff() {
    }

    public static Duration delayAfterFailureNumber(int failedAttemptsSoFar) {
        if (failedAttemptsSoFar <= 0) {
            return Duration.ZERO;
        }
        return switch (failedAttemptsSoFar) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            case 3 -> Duration.ofMinutes(15);
            default -> Duration.ofHours(1);
        };
    }
}
