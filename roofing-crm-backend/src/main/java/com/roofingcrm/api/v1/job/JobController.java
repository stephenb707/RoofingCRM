package com.roofingcrm.api.v1.job;

import com.roofingcrm.domain.enums.JobStatus;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.job.JobService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Validated
public class JobController {

    private final JobService jobService;

    @Autowired
    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobDto> createJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody CreateJobRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        JobDto created = jobService.createJob(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobDto> updateJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID jobId,
            @Valid @RequestBody UpdateJobRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        JobDto updated = jobService.updateJob(tenantId, userId, jobId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDto> getJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID jobId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        JobDto dto = jobService.getJob(tenantId, userId, jobId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<JobDto>> listJobs(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "status", required = false) JobStatus status,
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        Page<JobDto> page = jobService.listJobs(tenantId, userId, status, customerId, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<JobDto> updateJobStatus(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID jobId,
            @Valid @RequestBody UpdateJobStatusRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        JobDto updated = jobService.updateJobStatus(tenantId, userId, jobId, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
