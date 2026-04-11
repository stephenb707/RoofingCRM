package com.roofingcrm.api.v1.report;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.report.AccountingJobsReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports/accounting")
public class AccountingReportController {

    public static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final AccountingJobsReportService accountingJobsReportService;

    public AccountingReportController(AccountingJobsReportService accountingJobsReportService) {
        this.accountingJobsReportService = accountingJobsReportService;
    }

    @GetMapping(value = "/jobs.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadAccountingJobsXlsx(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        byte[] xlsx = accountingJobsReportService.generateAccountingJobsXlsx(tenantId, userId);
        String datePart = LocalDate.now(ZoneOffset.UTC).toString();
        String filename = "accounting-report-" + datePart + ".xlsx";
        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(XLSX_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(xlsx);
    }
}
