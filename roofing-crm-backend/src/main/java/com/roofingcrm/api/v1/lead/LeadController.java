package com.roofingcrm.api.v1.lead;

import com.roofingcrm.domain.enums.LeadStatus;
import com.roofingcrm.service.lead.LeadService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody CreateLeadRequest request) {

        LeadDto created = leadService.createLead(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LeadDto> updateLead(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable("id") UUID leadId,
            @Valid @RequestBody UpdateLeadRequest request) {

        LeadDto updated = leadService.updateLead(tenantId, userId, leadId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeadDto> getLead(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("id") UUID leadId) {

        LeadDto dto = leadService.getLead(tenantId, leadId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<LeadDto>> listLeads(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(value = "status", required = false) LeadStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<LeadDto> page = leadService.listLeads(tenantId, status, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<LeadDto> updateLeadStatus(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @PathVariable("id") UUID leadId,
            @Valid @RequestBody UpdateLeadStatusRequest request) {

        LeadDto updated = leadService.updateLeadStatus(tenantId, userId, leadId, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
