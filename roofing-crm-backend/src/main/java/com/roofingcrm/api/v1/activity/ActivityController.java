package com.roofingcrm.api.v1.activity;

import com.roofingcrm.domain.enums.ActivityEntityType;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.activity.ActivityEventService;
import com.roofingcrm.service.tenant.TenantAccessService;
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
@RequestMapping("/api/v1/activity")
@Validated
public class ActivityController {

    private final ActivityEventService activityEventService;
    private final TenantAccessService tenantAccessService;

    @Autowired
    public ActivityController(ActivityEventService activityEventService,
                              TenantAccessService tenantAccessService) {
        this.activityEventService = activityEventService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ResponseEntity<Page<ActivityEventDto>> listActivity(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam("entityType") ActivityEntityType entityType,
            @RequestParam("entityId") @NonNull UUID entityId,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        var tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        Page<ActivityEventDto> page = activityEventService.listEvents(tenant, entityType, entityId, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/notes")
    public ResponseEntity<ActivityEventDto> createNote(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody CreateNoteRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        var tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        ActivityEventDto created = activityEventService.createNote(
                tenant, userId, request.getEntityType(), request.getEntityId(), request.getBody());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
