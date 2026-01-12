package com.roofingcrm.api.v1.estimate;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.estimate.EstimateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
public class EstimateController {

    private final EstimateService estimateService;

    @Autowired
    public EstimateController(EstimateService estimateService) {
        this.estimateService = estimateService;
    }

    @PostMapping("/api/v1/jobs/{jobId}/estimates")
    public ResponseEntity<EstimateDto> createEstimate(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") UUID jobId,
            @Valid @RequestBody CreateEstimateRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        EstimateDto created = estimateService.createEstimateForJob(tenantId, userId, jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/api/v1/jobs/{jobId}/estimates")
    public ResponseEntity<List<EstimateDto>> listEstimatesForJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") UUID jobId) {

        List<EstimateDto> estimates = estimateService.listEstimatesForJob(tenantId, jobId);
        return ResponseEntity.ok(estimates);
    }

    @GetMapping("/api/v1/estimates/{id}")
    public ResponseEntity<EstimateDto> getEstimate(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID estimateId) {

        EstimateDto dto = estimateService.getEstimate(tenantId, estimateId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/api/v1/estimates/{id}")
    public ResponseEntity<EstimateDto> updateEstimate(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID estimateId,
            @Valid @RequestBody UpdateEstimateRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        EstimateDto updated = estimateService.updateEstimate(tenantId, userId, estimateId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/api/v1/estimates/{id}/status")
    public ResponseEntity<EstimateDto> updateEstimateStatus(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID estimateId,
            @Valid @RequestBody UpdateEstimateStatusRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        EstimateDto updated = estimateService.updateEstimateStatus(tenantId, userId, estimateId, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
