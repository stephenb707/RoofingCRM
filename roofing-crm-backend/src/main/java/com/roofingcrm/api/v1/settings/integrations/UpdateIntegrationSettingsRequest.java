package com.roofingcrm.api.v1.settings.integrations;

import jakarta.validation.constraints.Size;

import java.util.LinkedHashMap;
import java.util.Map;

public class UpdateIntegrationSettingsRequest {

    private Boolean enabled;

    @Size(max = 255)
    private String displayName;

    private Map<String, Object> config = new LinkedHashMap<>();

    /** Secret keys for this provider — never echoed back on GET. */
    private Map<String, String> secrets = new LinkedHashMap<>();

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config != null ? config : new LinkedHashMap<>();
    }

    public Map<String, String> getSecrets() {
        return secrets;
    }

    public void setSecrets(Map<String, String> secrets) {
        this.secrets = secrets != null ? secrets : new LinkedHashMap<>();
    }
}
