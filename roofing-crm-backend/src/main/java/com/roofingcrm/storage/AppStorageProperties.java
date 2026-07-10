package com.roofingcrm.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Selects attachment storage implementation. Local files are not migrated to S3 automatically;
 * prefer enabling S3 in production before collecting customer uploads.
 */
@Component
@ConfigurationProperties(prefix = "app.storage", ignoreUnknownFields = true)
public class AppStorageProperties {

    /**
     * {@code local} (default) or {@code s3}.
     */
    private StorageProvider provider = StorageProvider.LOCAL;

    public StorageProvider getProvider() {
        return provider;
    }

    public void setProvider(StorageProvider provider) {
        this.provider = provider;
    }

    public enum StorageProvider {
        LOCAL,
        S3
    }
}
