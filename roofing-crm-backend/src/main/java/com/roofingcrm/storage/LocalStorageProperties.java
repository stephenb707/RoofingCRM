package com.roofingcrm.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.storage.local")
public class LocalStorageProperties {

    /**
     * Base directory on disk where attachments are stored.
     */
    private String baseDir = "./uploads";

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }
}
