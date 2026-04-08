package com.roofingcrm.service.accounting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.receipt-extraction")
public class ReceiptExtractionProperties {

    private boolean enabled = false;
    private String provider = "openai";
    /**
     * When true, writes each processed summary image to java.io.tmpdir/roofing-crm-summary-debug/ for inspection.
     */
    private boolean debugWriteSummaryImages = false;
    private final OpenAi openai = new OpenAi();

    public boolean isDebugWriteSummaryImages() {
        return debugWriteSummaryImages;
    }

    public void setDebugWriteSummaryImages(boolean debugWriteSummaryImages) {
        this.debugWriteSummaryImages = debugWriteSummaryImages;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public static class OpenAi {
        private String baseUrl = "https://api.openai.com";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private int connectTimeoutSeconds = 10;
        private int readTimeoutSeconds = 45;
        private int maxRetries = 1;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getReadTimeoutSeconds() {
            return readTimeoutSeconds;
        }

        public void setReadTimeoutSeconds(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }
}
