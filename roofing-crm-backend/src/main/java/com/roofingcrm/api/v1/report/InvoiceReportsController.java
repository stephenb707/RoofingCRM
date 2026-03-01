package com.roofingcrm.api.v1.report;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.report.PaidInvoicesReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/invoices")
public class InvoiceReportsController {

    private final PaidInvoicesReportService paidInvoicesReportService;

    public InvoiceReportsController(PaidInvoicesReportService paidInvoicesReportService) {
        this.paidInvoicesReportService = paidInvoicesReportService;
    }

    @GetMapping("/paid/years")
    public ResponseEntity<List<Integer>> getPaidInvoiceYears(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<Integer> years = paidInvoicesReportService.getPaidInvoiceYears(tenantId, userId);
        return ResponseEntity.ok(years);
    }

    @GetMapping(value = "/paid", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> getPaidInvoicesPdf(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam("year") int year) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        byte[] pdf = paidInvoicesReportService.generatePaidInvoicesYearPdf(tenantId, userId, year);
        String filename = "paid-invoices-" + year + ".pdf";
        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
