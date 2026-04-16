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

        Map<String, Object> merged = new LinkedHashMap<>(entity.getPreferences());
        if (request.getDashboard() != null) merged.put("dashboard", request.getDashboard());
        if (request.getJobsList() != null) merged.put("jobsList", request.getJobsList());
        if (request.getLeadsList() != null) merged.put("leadsList", request.getLeadsList());
        if (request.getCustomersList() != null) merged.put("customersList", request.getCustomersList());
        if (request.getTasksList() != null) merged.put("tasksList", request.getTasksList());
        if (request.getEstimatesList() != null) merged.put("estimatesList", request.getEstimatesList());
        if (request.getPipeline() != null) merged.put("pipeline", request.getPipeline());
        entity.setPreferences(merged);

        TenantAppPreferences saved = preferencesRepository.save(entity);
        return toDto(saved);
    }

    private AppPreferencesDto toDto(TenantAppPreferences entity) {
        Map<String, Object> prefs = entity.getPreferences();
        AppPreferencesDto dto = new AppPreferencesDto();
        dto.setDashboard(resolveSection(prefs, "dashboard", AppPreferencesDefaults.dashboard()));
        dto.setJobsList(resolveSection(prefs, "jobsList", AppPreferencesDefaults.jobsList()));
        dto.setLeadsList(resolveSection(prefs, "leadsList", AppPreferencesDefaults.leadsList()));
        dto.setCustomersList(resolveSection(prefs, "customersList", AppPreferencesDefaults.customersList()));
        dto.setTasksList(resolveSection(prefs, "tasksList", AppPreferencesDefaults.tasksList()));
        dto.setEstimatesList(resolveSection(prefs, "estimatesList", AppPreferencesDefaults.estimatesList()));
        dto.setPipeline(resolveSection(prefs, "pipeline", AppPreferencesDefaults.pipeline()));
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    static AppPreferencesDto defaultDto() {
        AppPreferencesDto dto = new AppPreferencesDto();
        dto.setDashboard(AppPreferencesDefaults.dashboard());
        dto.setJobsList(AppPreferencesDefaults.jobsList());
        dto.setLeadsList(AppPreferencesDefaults.leadsList());
        dto.setCustomersList(AppPreferencesDefaults.customersList());
        dto.setTasksList(AppPreferencesDefaults.tasksList());
        dto.setEstimatesList(AppPreferencesDefaults.estimatesList());
        dto.setPipeline(AppPreferencesDefaults.pipeline());
        dto.setUpdatedAt(null);
        return dto;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resolveSection(Map<String, Object> prefs, String key,
                                                       Map<String, Object> defaults) {
        Object val = prefs.get(key);
        if (val instanceof Map<?, ?>) {
            return (Map<String, Object>) val;
        }
        return defaults;
    }

    private void requireAdmin(UUID tenantId, UUID userId) {
        tenantAccessService.requireAnyRole(
                Objects.requireNonNull(tenantId),
                Objects.requireNonNull(userId),
                Objects.requireNonNull(Set.of(UserRole.OWNER, UserRole.ADMIN)),
                "You do not have permission to manage app preferences.");
    }
}
