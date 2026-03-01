package com.roofingcrm.service.exception;

public class InvoiceLinkExpiredException extends RuntimeException {
    public InvoiceLinkExpiredException(String message) {
        super(message);
    }
}
