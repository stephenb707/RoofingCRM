package com.roofingcrm.api.v1.settings.integrations;

import com.roofingcrm.domain.enums.IntegrationProvider;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.settings.TenantIntegrationSettingsService;
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
@RequestMapping("/api/v1/settings/integrations")
@Validated
public class TenantIntegrationSettingsController {

    private final TenantIntegrationSettingsService tenantIntegrationSettingsService;

    @Autowired
    public TenantIntegrationSettingsController(TenantIntegrationSettingsService tenantIntegrationSettingsService) {
        this.tenantIntegrationSettingsService = tenantIntegrationSettingsService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationSettingsDto>> list(@RequestHeader("X-Tenant-Id") @NonNull UUID tenantId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return ResponseEntity.ok(tenantIntegrationSettingsService.listSettings(tenantId, userId));
    }

    @GetMapping("/{provider}")
    public ResponseEntity<IntegrationSettingsDto> get(@RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
                                                      @PathVariable("provider") String provider) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        IntegrationProvider p = parseProvider(provider);
        return ResponseEntity.ok(tenantIntegrationSettingsService.getSettings(tenantId, userId, p));
    }

    @PutMapping("/{provider}")
    public ResponseEntity<IntegrationSettingsDto> update(@RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
                                                        @PathVariable("provider") String provider,
                                                        @Valid @RequestBody UpdateIntegrationSettingsRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        IntegrationProvider p = parseProvider(provider);
        return ResponseEntity.ok(
                tenantIntegrationSettingsService.updateSettings(tenantId, userId, Objects.requireNonNull(p), Objects.requireNonNull(request)));
    }

    @PostMapping("/{provider}/disable")
    public ResponseEntity<IntegrationSettingsDto> disable(@RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
                                                         @PathVariable("provider") String provider) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        IntegrationProvider p = parseProvider(provider);
        return ResponseEntity.ok(tenantIntegrationSettingsService.disableIntegration(tenantId, userId, Objects.requireNonNull(p)));
    }

    private static IntegrationProvider parseProvider(String raw) {
        try {
            return IntegrationProvider.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown integration provider: " + raw);
        }
    }
}
