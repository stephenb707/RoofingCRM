package com.roofingcrm.api.v1.settings;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.settings.AppPreferencesService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings/preferences")
@Validated
public class AppPreferencesController {

    private final AppPreferencesService appPreferencesService;

    @Autowired
    public AppPreferencesController(AppPreferencesService appPreferencesService) {
        this.appPreferencesService = appPreferencesService;
    }

    @GetMapping
    public ResponseEntity<AppPreferencesDto> get(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(appPreferencesService.getPreferences(tenantId, userId));
    }

    @PutMapping
    public ResponseEntity<AppPreferencesDto> update(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody UpdateAppPreferencesRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(appPreferencesService.updatePreferences(tenantId, userId, request));
    }
}
