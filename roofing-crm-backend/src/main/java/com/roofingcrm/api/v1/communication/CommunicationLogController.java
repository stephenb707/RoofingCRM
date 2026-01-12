package com.roofingcrm.api.v1.communication;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.communication.CommunicationLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class CommunicationLogController {

    private final CommunicationLogService communicationLogService;

    public CommunicationLogController(CommunicationLogService communicationLogService) {
        this.communicationLogService = communicationLogService;
    }

    @PostMapping("/leads/{leadId}/communications")
    public ResponseEntity<CommunicationLogDto> addForLead(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("leadId") @NonNull UUID leadId,
            @Valid @RequestBody CreateCommunicationLogRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        CommunicationLogDto dto = communicationLogService.addForLead(tenantId, userId, leadId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/leads/{leadId}/communications")
    public ResponseEntity<List<CommunicationLogDto>> listForLead(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("leadId") @NonNull UUID leadId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<CommunicationLogDto> logs = communicationLogService.listForLead(tenantId, userId, leadId);
        return ResponseEntity.ok(logs);
    }

    @PostMapping("/jobs/{jobId}/communications")
    public ResponseEntity<CommunicationLogDto> addForJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") @NonNull UUID jobId,
            @Valid @RequestBody CreateCommunicationLogRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        CommunicationLogDto dto = communicationLogService.addForJob(tenantId, userId, jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/jobs/{jobId}/communications")
    public ResponseEntity<List<CommunicationLogDto>> listForJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") @NonNull UUID jobId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<CommunicationLogDto> logs = communicationLogService.listForJob(tenantId, userId, jobId);
        return ResponseEntity.ok(logs);
    }
}
