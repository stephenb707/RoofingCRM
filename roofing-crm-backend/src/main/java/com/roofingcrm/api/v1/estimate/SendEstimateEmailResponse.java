package com.roofingcrm.api.v1.estimate;

import java.time.Instant;

public class SendEstimateEmailResponse {

    private boolean success;
    private Instant sentAt;
    private String publicUrl;
    private boolean reusedExistingToken;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public boolean isReusedExistingToken() {
        return reusedExistingToken;
    }

    public void setReusedExistingToken(boolean reusedExistingToken) {
        this.reusedExistingToken = reusedExistingToken;
    }
}
