package com.roofingcrm.api.v1.lead;

import com.roofingcrm.api.v1.common.PickerItemDto;
import com.roofingcrm.api.v1.job.JobDto;
import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.lead.LeadService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leads")
@Validated
public class LeadController {

    private final LeadService leadService;

    @Autowired
    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public ResponseEntity<LeadDto> createLead(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody CreateLeadRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        LeadDto created = leadService.createLead(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadDto> updateLead(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID leadId,
            @Valid @RequestBody UpdateLeadRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        LeadDto updated = leadService.updateLead(tenantId, userId, leadId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadDto> getLead(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID leadId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        LeadDto dto = leadService.getLead(tenantId, userId, leadId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/picker")
    public ResponseEntity<List<PickerItemDto>> searchLeadsPicker(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<PickerItemDto> items = leadService.searchLeadsForPicker(tenantId, userId, q, Math.min(Math.max(limit, 1), 50));
        return ResponseEntity.ok(items);
    }

    @GetMapping
    public ResponseEntity<Page<LeadDto>> listLeads(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "status", required = false) LeadStatus status,
            @RequestParam(value = "customerId", required = false) UUID customerId,
            @PageableDefault(size = 20) @NonNull Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        Page<LeadDto> page = leadService.listLeads(tenantId, userId, status, customerId, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<LeadDto> updateLeadStatus(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID leadId,
            @Valid @RequestBody UpdateLeadStatusRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        LeadDto updated = leadService.updateLeadStatus(tenantId, userId, leadId, request.getStatus(), request.getPosition());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<JobDto> convertLeadToJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID leadId,
            @Valid @RequestBody ConvertLeadToJobRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        JobDto job = leadService.convertLeadToJob(tenantId, userId, leadId, request);
        return ResponseEntity.ok(job);
    }
}
