package com.roofingcrm.api.v1.report;

import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.domain.enums.LeadSource;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.report.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @Autowired
    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(value = "/leads.csv", produces = "text/csv; charset=utf-8")
    public ResponseEntity<byte[]> exportLeadsCsv(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "status", required = false) LeadStatus status,
            @RequestParam(value = "source", required = false) LeadSource source,
            @RequestParam(value = "limit", defaultValue = "2000") int limit) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        int capped = Math.min(Math.max(limit, 1), 5000);
        byte[] csv = reportService.exportLeadsCsv(userId, tenantId, status, source, capped);

        String filename = "leads-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }

    @GetMapping(value = "/jobs.csv", produces = "text/csv; charset=utf-8")
    public ResponseEntity<byte[]> exportJobsCsv(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "status", required = false) JobStatus status,
            @RequestParam(value = "limit", defaultValue = "2000") int limit) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        int capped = Math.min(Math.max(limit, 1), 5000);
        byte[] csv = reportService.exportJobsCsv(userId, tenantId, status, capped);

        String filename = "jobs-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(csv);
    }
}
