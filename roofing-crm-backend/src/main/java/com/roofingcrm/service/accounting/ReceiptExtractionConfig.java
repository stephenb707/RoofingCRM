package com.roofingcrm.service.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ReceiptExtractionConfig {

    private static final Logger log = LoggerFactory.getLogger(ReceiptExtractionConfig.class);
    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    @Bean
    ReceiptExtractionClient receiptExtractionClient(RestClient.Builder restClientBuilder,
                                                    ObjectMapper objectMapper,
                                                    ReceiptExtractionProperties properties) {
        boolean enabled = properties.isEnabled();
        String provider = normalize(properties.getProvider());
        String model = normalizeModel(properties.getOpenai().getModel());
        String baseUrl = normalizeBaseUrl(properties.getOpenai().getBaseUrl());
        String apiKey = trimToNull(properties.getOpenai().getApiKey());
        boolean apiKeyPresent = apiKey != null;

        if (!enabled) {
            return noop(enabled, provider, apiKeyPresent, model,
                    "disabled",
                    "Receipt extraction is disabled. Set APP_RECEIPT_EXTRACTION_ENABLED=true.");
        }

        if (!"openai".equals(provider)) {
            return noop(enabled, provider, apiKeyPresent, model,
                    "provider missing/unsupported",
                    "Receipt extraction provider is not supported. Set APP_RECEIPT_EXTRACTION_PROVIDER=openai.");
        }

        if (!apiKeyPresent) {
            return noop(enabled, provider, false, model,
                    "api key missing",
                    "Receipt extraction is enabled but APP_RECEIPT_EXTRACTION_OPENAI_API_KEY is missing.");
        }

        try {
            ReceiptExtractionClient client =
                    new OpenAiReceiptExtractionClient(
                            restClientBuilder,
                            objectMapper,
                            baseUrl,
                            apiKey,
                            model,
                            properties.getOpenai().getConnectTimeoutSeconds(),
                            properties.getOpenai().getReadTimeoutSeconds(),
                            properties.getOpenai().getMaxRetries());
            log.info(
                    "Receipt extraction config: enabled={}, provider={}, apiKeyPresent={}, model={}, connectTimeoutSeconds={}, readTimeoutSeconds={}, maxRetries={}, selectedClient=OPENAI",
                    enabled,
                    provider,
                    apiKeyPresent,
                    model,
                    properties.getOpenai().getConnectTimeoutSeconds(),
                    properties.getOpenai().getReadTimeoutSeconds(),
                    properties.getOpenai().getMaxRetries());
            return client;
        } catch (RuntimeException ex) {
            return noop(enabled, provider, apiKeyPresent, model,
                    "bean construction failure",
                    "Receipt extraction client failed to initialize: " + ex.getMessage());
        }
    }

    private ReceiptExtractionClient noop(boolean enabled,
                                         String provider,
                                         boolean apiKeyPresent,
                                         String model,
                                         String reason,
                                         String message) {
        log.warn(
                "Receipt extraction config: enabled={}, provider={}, apiKeyPresent={}, model={}, selectedClient=NOOP, reason={}",
                enabled, provider, apiKeyPresent, model, reason);
        log.warn("Receipt extraction NOOP reason detail: {}", message);
        return new NoopReceiptExtractionClient(message);
    }

    private static String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toLowerCase();
    }

    private static String normalizeModel(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? DEFAULT_MODEL : trimmed;
    }

    private static String normalizeBaseUrl(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "https://api.openai.com" : trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
