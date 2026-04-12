package com.roofingcrm.api.v1.settings;

import com.roofingcrm.domain.enums.PipelineType;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.pipeline.PipelineStatusAdminService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings/pipeline-statuses")
@Validated
public class SettingsPipelineStatusController {

    private final PipelineStatusAdminService pipelineStatusAdminService;

    @Autowired
    public SettingsPipelineStatusController(PipelineStatusAdminService pipelineStatusAdminService) {
        this.pipelineStatusAdminService = pipelineStatusAdminService;
    }

    @GetMapping
    public ResponseEntity<List<PipelineStatusDefinitionDto>> list(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam("type") PipelineType type) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(pipelineStatusAdminService.list(tenantId, userId, Objects.requireNonNull(type)));
    }

    @PostMapping
    public ResponseEntity<PipelineStatusDefinitionDto> create(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody CreatePipelineStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(pipelineStatusAdminService.create(tenantId, userId, Objects.requireNonNull(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PipelineStatusDefinitionDto> update(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePipelineStatusRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(pipelineStatusAdminService.update(tenantId, userId, Objects.requireNonNull(id), Objects.requireNonNull(request)));
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody ReorderPipelineStatusesRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        pipelineStatusAdminService.reorder(tenantId, userId, Objects.requireNonNull(request));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/restore-defaults")
    public ResponseEntity<Void> restoreDefaults(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam("type") PipelineType type,
            @RequestParam(value = "deactivateUnusedCustom", defaultValue = "false") boolean deactivateUnusedCustom) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        pipelineStatusAdminService.restoreDefaults(tenantId, userId, Objects.requireNonNull(type), deactivateUnusedCustom);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        pipelineStatusAdminService.deactivate(tenantId, userId, Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }
}
