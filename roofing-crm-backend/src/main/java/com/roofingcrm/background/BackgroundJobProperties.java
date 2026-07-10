package com.roofingcrm.background;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Poll-based worker that claims integration background jobs. Safe for a single backend instance; multiple
 * instances may double-process unless claim/locking is extended (e.g. leases, leader election).
 */
@Component
@ConfigurationProperties(prefix = "app.background-jobs")
public class BackgroundJobProperties {

    private boolean enabled = true;
    private int pollIntervalSeconds = 30;
    private int batchSize = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPollIntervalSeconds() {
        return pollIntervalSeconds;
    }

    public void setPollIntervalSeconds(int pollIntervalSeconds) {
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /** Milliseconds for Spring {@code @Scheduled(fixedDelayString = "...")}. */
    public long getPollDelayMs() {
        return Math.max(1, pollIntervalSeconds) * 1000L;
    }
}
