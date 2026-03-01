package com.roofingcrm.service.exception;

public class NoPaidInvoicesForYearException extends RuntimeException {
    public NoPaidInvoicesForYearException(int year) {
        super("No paid invoices found for year " + year + ".");
    }
}
