package com.roofingcrm.security.ratelimit;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Fixed 60-second window per key. In-memory only; not shared across horizontally scaled instances.
 * Entries for keys inactive longer than the previous minute window are removed periodically so the
 * map does not grow without bound as new keys appear.
 */
public class MinuteWindowRateLimiter {

    private static final long WINDOW_MS = 60_000L;
    private static final long PRUNE_INTERVAL_MS = 60_000L;
    private static final int PRUNE_SIZE_PRESSURE = 4096;

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final LongSupplier epochMillis;
    private volatile long lastPruneEpochMs;

    public MinuteWindowRateLimiter() {
        this(System::currentTimeMillis);
    }

    MinuteWindowRateLimiter(LongSupplier epochMillis) {
        this.epochMillis = Objects.requireNonNull(epochMillis);
        this.lastPruneEpochMs = epochMillis.getAsLong();
    }

    /**
     * @return true if the request is allowed, false if the limit for the current minute is exceeded.
     */
    public boolean tryAcquire(String scopeKey, int maxPerMinute) {
        if (maxPerMinute <= 0) {
            return false;
        }
        long nowMs = epochMillis.getAsLong();
        long window = nowMs / WINDOW_MS;
        maybePruneUnused(nowMs, window);
        Counter c = counters.computeIfAbsent(scopeKey, k -> new Counter());
        synchronized (c) {
            if (c.window != window) {
                c.window = window;
                c.count = 0;
            }
            if (c.count >= maxPerMinute) {
                return false;
            }
            c.count++;
            return true;
        }
    }

    private void maybePruneUnused(long nowMs, long currentWindow) {
        int size = counters.size();
        if (size == 0) {
            return;
        }
        boolean sizePressure = size >= PRUNE_SIZE_PRESSURE;
        boolean timeDue = (nowMs - lastPruneEpochMs) >= PRUNE_INTERVAL_MS;
        if (!sizePressure && !timeDue) {
            return;
        }
        lastPruneEpochMs = nowMs;
        pruneStaleWindows(currentWindow);
    }

    /**
     * Drops keys whose last active window is older than the previous minute (keeps current and prior minute).
     */
    private void pruneStaleWindows(long currentWindow) {
        long oldestRetainedWindow = currentWindow - 1;
        counters.entrySet().removeIf(e -> {
            Counter c = e.getValue();
            synchronized (c) {
                if (c.window < 0) {
                    return false;
                }
                return c.window < oldestRetainedWindow;
            }
        });
    }

    int testingEntryCount() {
        return counters.size();
    }

    private static final class Counter {
        private long window = -1L;
        private int count;
    }
}
