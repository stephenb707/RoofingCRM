package com.roofingcrm.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for S3-compatible object storage (AWS S3, Cloudflare R2, MinIO, etc.).
 */
@Component
@ConfigurationProperties(prefix = "app.storage.s3")
public class S3AttachmentStorageProperties {

    private String bucket = "";
    private String region = "us-east-1";
    private String prefix = "";
    private String accessKey = "";
    private String secretKey = "";
    /** Optional custom endpoint (R2, MinIO). Leave blank for AWS default. */
    private String endpoint = "";
    private boolean pathStyleAccess = false;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public boolean isPathStyleAccess() {
        return pathStyleAccess;
    }

    public void setPathStyleAccess(boolean pathStyleAccess) {
        this.pathStyleAccess = pathStyleAccess;
    }
}
