package com.roofingcrm.integrations;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Refuses to start without a real encryption key when a production-like profile is active.
 */
@Component
public class IntegrationSecretProtectorStartupChecker {

    private final Environment environment;
    private final IntegrationEncryptionProperties encryptionProperties;

    public IntegrationSecretProtectorStartupChecker(Environment environment,
                                                   IntegrationEncryptionProperties encryptionProperties) {
        this.environment = environment;
        this.encryptionProperties = encryptionProperties;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        boolean prodLike = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
        if (!prodLike) {
            return;
        }
        String key = encryptionProperties.getEncryptionKey();
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "APP_INTEGRATIONS_ENCRYPTION_KEY (app.integrations.encryption-key) is required when running with a production profile.");
        }
    }
}
