package com.roofingcrm.api.v1.customerreport;

import java.time.Instant;

public class SendCustomerPhotoReportEmailResponse {

    private boolean success;
    private Instant sentAt;

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
}
