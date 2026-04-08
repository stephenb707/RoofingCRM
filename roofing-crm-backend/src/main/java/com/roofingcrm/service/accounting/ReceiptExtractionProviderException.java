package com.roofingcrm.service.accounting;

public class ReceiptExtractionProviderException extends RuntimeException {

    public ReceiptExtractionProviderException(String message) {
        super(message);
    }

    public ReceiptExtractionProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
