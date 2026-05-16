package com.roofingcrm.service.settings;

import com.roofingcrm.api.v1.settings.AppPreferencesDto;
import com.roofingcrm.api.v1.settings.UpdateAppPreferencesRequest;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.entity.TenantAppPreferences;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.TenantAppPreferencesRepository;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class AppPreferencesServiceImpl implements AppPreferencesService {

    private final TenantAccessService tenantAccessService;
    private final TenantAppPreferencesRepository preferencesRepository;

    @Autowired
    public AppPreferencesServiceImpl(
            TenantAccessService tenantAccessService,
            TenantAppPreferencesRepository preferencesRepository) {
        this.tenantAccessService = tenantAccessService;
        this.preferencesRepository = preferencesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AppPreferencesDto getPreferences(@NonNull UUID tenantId, @NonNull UUID userId) {
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        return preferencesRepository.findByTenant(tenant)
                .map(this::toDto)
                .orElseGet(AppPreferencesServiceImpl::defaultDto);
    }

    @Override
    public AppPreferencesDto updatePreferences(@NonNull UUID tenantId, @NonNull UUID userId,
                                                @NonNull UpdateAppPreferencesRequest request) {
        requireAdmin(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);

        TenantAppPreferences entity = preferencesRepository.findByTenant(tenant)
                .orElseGet(() -> {
                    TenantAppPreferences fresh = new TenantAppPreferences();
                    fresh.setTenant(tenant);
                    fresh.setPreferences(new LinkedHashMap<>());
                    return fresh;
                });

        Map<String, Object> merged = new LinkedHashMap<>();
        if (entity.getPreferences() != null) {
            merged.putAll(entity.getPreferences());
        }
        if (request.getDashboard() != null) {
            merged.put("dashboard", request.getDashboard());
        }
        if (request.getJobsList() != null) {
            merged.put("jobsList", request.getJobsList());
        }
        if (request.getLeadsList() != null) {
            merged.put("leadsList", request.getLeadsList());
        }
        if (request.getCustomersList() != null) {
            merged.put("customersList", request.getCustomersList());
        }
        if (request.getTasksList() != null) {
            merged.put("tasksList", request.getTasksList());
        }
        if (request.getEstimatesList() != null) {
            merged.put("estimatesList", request.getEstimatesList());
        }
        if (request.getPipeline() != null) {
            merged.put("pipeline", request.getPipeline());
        }

        entity.setPreferences(buildSanitizedSnapshot(merged));

        TenantAppPreferences saved = preferencesRepository.save(entity);
        return toDto(saved);
    }

    private AppPreferencesDto toDto(TenantAppPreferences entity) {
        Map<String, Object> prefs = entity.getPreferences() != null ? entity.getPreferences() : Map.of();
        AppPreferencesDto dto = new AppPreferencesDto();
        dto.setDashboard(AppPreferencesSanitizer.sanitizeDashboard(prefs.get("dashboard")));
        dto.setJobsList(AppPreferencesSanitizer.sanitizeJobsList(prefs.get("jobsList")));
        dto.setLeadsList(AppPreferencesSanitizer.sanitizeLeadsList(prefs.get("leadsList")));
        dto.setCustomersList(AppPreferencesSanitizer.sanitizeCustomersList(prefs.get("customersList")));
        dto.setTasksList(AppPreferencesSanitizer.sanitizeTasksList(prefs.get("tasksList")));
        dto.setEstimatesList(AppPreferencesSanitizer.sanitizeEstimatesList(prefs.get("estimatesList")));
        dto.setPipeline(AppPreferencesSanitizer.sanitizePipeline(prefs.get("pipeline")));
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    static AppPreferencesDto defaultDto() {
        AppPreferencesDto dto = new AppPreferencesDto();
        dto.setDashboard(AppPreferencesSanitizer.sanitizeDashboard(null));
        dto.setJobsList(AppPreferencesSanitizer.sanitizeJobsList(null));
        dto.setLeadsList(AppPreferencesSanitizer.sanitizeLeadsList(null));
        dto.setCustomersList(AppPreferencesSanitizer.sanitizeCustomersList(null));
        dto.setTasksList(AppPreferencesSanitizer.sanitizeTasksList(null));
        dto.setEstimatesList(AppPreferencesSanitizer.sanitizeEstimatesList(null));
        dto.setPipeline(AppPreferencesSanitizer.sanitizePipeline(null));
        dto.setUpdatedAt(null);
        return dto;
    }

    /**
     * Normalizes known preference sections and drops unknown top-level keys from merged state.
     */
    static Map<String, Object> buildSanitizedSnapshot(Map<String, Object> merged) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : merged.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            switch (key) {
                case "dashboard" -> out.put(key, AppPreferencesSanitizer.sanitizeDashboard(val));
                case "jobsList" -> out.put(key, AppPreferencesSanitizer.sanitizeJobsList(val));
                case "leadsList" -> out.put(key, AppPreferencesSanitizer.sanitizeLeadsList(val));
                case "customersList" -> out.put(key, AppPreferencesSanitizer.sanitizeCustomersList(val));
                case "tasksList" -> out.put(key, AppPreferencesSanitizer.sanitizeTasksList(val));
                case "estimatesList" -> out.put(key, AppPreferencesSanitizer.sanitizeEstimatesList(val));
                case "pipeline" -> out.put(key, AppPreferencesSanitizer.sanitizePipeline(val));
                default -> {
                    /* ignore unknown / legacy top-level keys */
                }
            }
        }
        return out;
    }

    private void requireAdmin(UUID tenantId, UUID userId) {
        tenantAccessService.requireAnyRole(
                Objects.requireNonNull(tenantId),
                Objects.requireNonNull(userId),
                Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN)),
                "You do not have permission to manage app preferences.");
    }
}
