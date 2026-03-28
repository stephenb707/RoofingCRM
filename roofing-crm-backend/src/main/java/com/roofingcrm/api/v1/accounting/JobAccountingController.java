package com.roofingcrm.api.v1.accounting;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.accounting.JobAccountingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
public class JobAccountingController {

    private final JobAccountingService jobAccountingService;

    @Autowired
    public JobAccountingController(JobAccountingService jobAccountingService) {
        this.jobAccountingService = jobAccountingService;
    }

    @GetMapping("/api/v1/jobs/{jobId}/accounting/summary")
    public ResponseEntity<JobAccountingSummaryDto> getJobAccountingSummary(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") UUID jobId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(jobAccountingService.getJobAccountingSummary(tenantId, userId, jobId));
    }

    @GetMapping("/api/v1/jobs/{jobId}/costs")
    public ResponseEntity<List<JobCostEntryDto>> listJobCostEntries(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") UUID jobId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(jobAccountingService.listJobCostEntries(tenantId, userId, jobId));
    }

    @PostMapping("/api/v1/jobs/{jobId}/costs")
    public ResponseEntity<JobCostEntryDto> createJobCostEntry(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") UUID jobId,
            @Valid @RequestBody CreateJobCostEntryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        JobCostEntryDto created = jobAccountingService.createJobCostEntry(tenantId, userId, jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/api/v1/jobs/{jobId}/costs/{costEntryId}")
    public ResponseEntity<JobCostEntryDto> updateJobCostEntry(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") UUID jobId,
            @PathVariable("costEntryId") UUID costEntryId,
            @Valid @RequestBody UpdateJobCostEntryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(jobAccountingService.updateJobCostEntry(tenantId, userId, jobId, costEntryId, request));
    }

    @DeleteMapping("/api/v1/jobs/{jobId}/costs/{costEntryId}")
    public ResponseEntity<Void> deleteJobCostEntry(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") UUID jobId,
            @PathVariable("costEntryId") UUID costEntryId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        jobAccountingService.deleteJobCostEntry(tenantId, userId, jobId, costEntryId);
        return ResponseEntity.noContent().build();
    }
}
