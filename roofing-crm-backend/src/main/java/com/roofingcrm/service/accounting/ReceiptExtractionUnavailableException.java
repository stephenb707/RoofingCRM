package com.roofingcrm.service.accounting;

public class ReceiptExtractionUnavailableException extends RuntimeException {

    public ReceiptExtractionUnavailableException(String message) {
        super(message);
    }

    public ReceiptExtractionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
