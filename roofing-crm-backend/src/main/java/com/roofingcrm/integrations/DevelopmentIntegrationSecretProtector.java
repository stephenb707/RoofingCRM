package com.roofingcrm.integrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base64 obfuscation only — suitable for local development when no encryption key is configured.
 */
public class DevelopmentIntegrationSecretProtector implements IntegrationSecretProtector {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentIntegrationSecretProtector.class);
    private static final String PREFIX = "DEV:";

    public DevelopmentIntegrationSecretProtector() {
        log.warn("APP_INTEGRATIONS_ENCRYPTION_KEY is not set — integration secrets use a non-production protector (DevelopmentIntegrationSecretProtector)");
    }

    @Override
    public String protect(String plaintextUtf8) {
        return PREFIX + Base64.getEncoder().encodeToString(plaintextUtf8.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String unprotect(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Unrecognized dev ciphertext");
        }
        return new String(Base64.getDecoder().decode(ciphertext.substring(PREFIX.length())), StandardCharsets.UTF_8);
    }
}
