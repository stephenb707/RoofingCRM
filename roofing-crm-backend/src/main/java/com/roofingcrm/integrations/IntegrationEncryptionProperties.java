package com.roofingcrm.integrations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.integrations")
public class IntegrationEncryptionProperties {

    /**
     * Passphrase for AES-256-GCM encryption of integration secrets. Use a long random value in production.
     */
    private String encryptionKey = "";

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
}
