package com.roofingcrm.api.v1.settings;

import com.roofingcrm.domain.enums.PipelineType;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.pipeline.PipelineStatusReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only pipeline status definitions for building pipeline columns (any tenant member).
 */
@RestController
@RequestMapping("/api/v1/pipeline-statuses")
@Validated
public class PipelineStatusController {

    private final PipelineStatusReadService pipelineStatusReadService;

    @Autowired
    public PipelineStatusController(PipelineStatusReadService pipelineStatusReadService) {
        this.pipelineStatusReadService = pipelineStatusReadService;
    }

    @GetMapping
    public ResponseEntity<List<PipelineStatusDefinitionDto>> listActive(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam("type") PipelineType type) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(pipelineStatusReadService.listActive(tenantId, userId, Objects.requireNonNull(type)));
    }
}
