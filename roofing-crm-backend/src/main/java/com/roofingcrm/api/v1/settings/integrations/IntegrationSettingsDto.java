package com.roofingcrm.api.v1.settings.integrations;

import com.roofingcrm.domain.enums.IntegrationConnectionStatus;
import com.roofingcrm.domain.enums.IntegrationProvider;

import java.time.Instant;
import java.util.Map;

public class IntegrationSettingsDto {

    private IntegrationProvider provider;
    private String humanLabel;
    private String category;
    private boolean enabled;
    private IntegrationConnectionStatus status;
    private String displayName;
    private boolean hasCredentials;
    private Instant lastConnectedAt;
    private String lastError;
    private Map<String, Object> config;
    private Instant updatedAt;

    public IntegrationProvider getProvider() {
        return provider;
    }

    public void setProvider(IntegrationProvider provider) {
        this.provider = provider;
    }

    public String getHumanLabel() {
        return humanLabel;
    }

    public void setHumanLabel(String humanLabel) {
        this.humanLabel = humanLabel;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public IntegrationConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(IntegrationConnectionStatus status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isHasCredentials() {
        return hasCredentials;
    }

    public void setHasCredentials(boolean hasCredentials) {
        this.hasCredentials = hasCredentials;
    }

    public Instant getLastConnectedAt() {
        return lastConnectedAt;
    }

    public void setLastConnectedAt(Instant lastConnectedAt) {
        this.lastConnectedAt = lastConnectedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
